package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.AutoKnockoffDocumentResponse
import com.cogoport.ares.api.payment.model.PaymentUtilizationResponse
import com.cogoport.ares.api.payment.model.ReversePaymentRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.TaggedSettlementService
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Inject
import java.math.BigDecimal
import java.sql.SQLException
import java.util.UUID
import javax.transaction.Transactional

class TaggedSettlementServiceImpl : TaggedSettlementService {

    @Inject
    private lateinit var settlementRepository: SettlementRepository

    @Inject
    private lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    private lateinit var auditService: AuditService

    @Inject
    private lateinit var settlementServiceHelper: SettlementServiceHelper

    @Inject
    private lateinit var settlementServiceImpl: SettlementServiceImpl

    @Inject
    private lateinit var documentConverter: DocumentMapper

    @Inject
    private lateinit var aresMessagePublisher: AresMessagePublisher

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun settleOnAccountInvoicePayment(req: OnAccountPaymentRequest) {
        val destinationDocument = AutoKnockoffDocumentResponse()
        val extendedSourceDocument = mutableListOf<AutoKnockoffDocumentResponse?>()
        req.document.documentNo = Hashids.decode(req.document.documentNo)[0].toString()
        val taggedDocumentIds = req.taggedDocuments.map { Hashids.decode(it.documentNo)[0] }
        // TODO("only have to use amount of pcn or pay which is settled")
        val settledSourceDocuments = settlementRepository.getPaymentsCorrespondingDocumentNo(taggedDocumentIds)
        val sourceType: List<String?> = settledSourceDocuments.map { it?.sourceType.toString() }.distinct()
        val sourceAccountUtilization = accountUtilizationRepository.findRecords(
            settledSourceDocuments.map { it?.sourceId!!.toLong() },
            sourceType,
            null
        )
        sourceAccountUtilization.forEach { source ->
            val paymentInfo = settledSourceDocuments.find { it?.sourceId!!.toLong() == source?.documentNo }
            val documentInfo = req.taggedDocuments.find { Hashids.decode(it.documentNo)[0] == paymentInfo?.destinationId!!.toLong() }
            val isSettled = paymentInfo?.transRefNumber != documentInfo?.transactionNo && paymentInfo?.sourceType == SettlementType.PAY
            extendedSourceDocument.add(
                AutoKnockoffDocumentResponse(
                    accountUtilization = source,
                    paidTds = documentInfo?.paidTds,
                    payableTds = documentInfo?.payableTds,
                    exchangeRate = documentInfo?.exchangeRate!!,
                    isSettled = isSettled,
                    amount = paymentInfo?.amount!!
                )
            )
        }
        extendedSourceDocument.sortedByDescending { it?.accountUtilization?.amountCurr!! - it.accountUtilization?.payCurr!! }
        destinationDocument.accountUtilization = accountUtilizationRepository.findRecord(
            req.document.documentNo.toLong(),
            AccountType.PINV.name,
            AccMode.AP.name
        )
        destinationDocument.payableTds = req.document.payableTds
        destinationDocument.paidTds = req.document.paidTds
        destinationDocument.exchangeRate = req.document.exchangeRate
        var balanceSettlingAmount = destinationDocument.accountUtilization?.amountCurr!!.minus(destinationDocument.accountUtilization?.payCurr!!)
        var sourceStartIndex = 0
        val sourceEndIndex = extendedSourceDocument.size
        while (req.settledAmount + req.settledTds >= BigDecimal.ZERO && balanceSettlingAmount > BigDecimal.ZERO && sourceStartIndex < sourceEndIndex) {
            var settledList: List<CheckDocument>?
            val source = extendedSourceDocument[sourceStartIndex]
            var balancePaymentUtilization = BigDecimal.ZERO
            if (!source?.isSettled!!) {
                balancePaymentUtilization = source.accountUtilization?.amountCurr!! - source.accountUtilization?.payCurr!!
            }

            if ((balancePaymentUtilization > BigDecimal.ZERO)) {
                settledList = settleTaggedDocument(destinationDocument, source, req.createdBy)
                destinationDocument.accountUtilization!!.payCurr += settledList?.get(1)?.settledAmount!!
                destinationDocument.accountUtilization!!.payLoc += settledList[1].settledAmount!! * settledList[1].exchangeRate
            } else {
                val payment = settledSourceDocuments.find { it?.sourceId!!.toLong() == source.accountUtilization?.documentNo }
                val reversePaymentAmount = if (payment?.amount!! >= req.settledAmount && balanceSettlingAmount >= req.settledAmount) req.settledAmount else if (payment.amount < req.settledAmount && balanceSettlingAmount >= payment.amount) payment.amount else balanceSettlingAmount
                val availablePayment = reversePayment(
                    ReversePaymentRequest(
                        payment.destinationId.toLong(),
                        payment.sourceId.toLong(),
                        reversePaymentAmount,
                        source.exchangeRate,
                        req.createdBy,
                        null
                    )
                )
                source.accountUtilization!!.payCurr = availablePayment.payCurr
                source.accountUtilization!!.payLoc = availablePayment.payLoc
                source.amount = reversePaymentAmount
                settledList = settleTaggedDocument(destinationDocument, source, req.createdBy)
                destinationDocument.accountUtilization!!.payCurr += (settledList?.get(1)?.settledAllocation!! + settledList[1].settledAmount!!)
                destinationDocument.accountUtilization!!.payLoc += (settledList[1].settledAllocation + settledList[1].settledAmount!!) * settledList[1].exchangeRate
            }
            balanceSettlingAmount = (settledList[1].settledAllocation + settledList[1].settledAmount!!) // TODO(wrong balanceAmount)
            req.settledAmount -= settledList[1].settledAllocation
            req.settledTds -= settledList[1].settledTds // TODO(wrong settledTds)
            sourceStartIndex++
        }
    }
    private suspend fun reversePayment(reversePaymentRequest: ReversePaymentRequest): PaymentUtilizationResponse {
        val accountUtilization = accountUtilizationRepository.findRecord(reversePaymentRequest.document, AccountType.PINV.name, AccMode.AP.name)
        val settlements = settlementRepository.getSettlementDetailsByDestinationId(accountUtilization!!.documentNo, reversePaymentRequest.source)
        val amount = settlements.sumOf { it.amount!! }
        val settlementIds = settlements.map { it.id!! }
        settlementRepository.markSettlementIsDraftTrue(settlementIds)
// TODO(which amount have to pass in 141, 142; amount or reversePaymentRequest.amount)
        val accountUtilizationPaymentData = accountUtilizationRepository.getDataByPaymentNum(reversePaymentRequest.source)
        accountUtilizationRepository.markPaymentUnutilized(accountUtilizationPaymentData.id, reversePaymentRequest.amount, reversePaymentRequest.amount * reversePaymentRequest.exchangeRate) // TODO("nothing")
        accountUtilizationRepository.updateAccountUtilizations(accountUtilization.id!!, true)
        accountUtilizationPaymentData.payCurr -= amount
        accountUtilizationPaymentData.payLoc -= (amount * reversePaymentRequest.exchangeRate)
        createAudit(AresConstants.SETTLEMENT, settlementIds[0], AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.SETTLEMENT, settlementIds[1], AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilizationPaymentData.id, AresConstants.UPDATE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilization.id!!, AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)

//        try {
//            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accountUtilization.organizationId))
//        } catch (e: Exception) {
//            Sentry.captureException(e)
//        }
        return accountUtilizationPaymentData
    }

    private suspend fun createAudit(
        objectType: String,
        objectId: Long?,
        actionName: String,
        data: Any?,
        performedBy: String,
        performedByUserType: String?

    ) {
        auditService.createAudit(
            AuditRequest(
                objectType = objectType,
                objectId = objectId,
                actionName = actionName,
                data = data,
                performedBy = performedBy,
                performedByUserType = performedByUserType
            )
        )
    }
    private suspend fun settleTaggedDocument(destinationDocument: AutoKnockoffDocumentResponse, sourceDocument: AutoKnockoffDocumentResponse, createdBy: UUID?): List<CheckDocument>? {
        val listOfDocuments = mutableListOf<AutoKnockoffDocumentResponse>()
        listOfDocuments.add(sourceDocument)
        listOfDocuments.add(destinationDocument)
        var settled: List<CheckDocument>? = null
        if (listOfDocuments.isNotEmpty()) {
            val documentEntity = listOfDocuments.map {
                val doc = it.accountUtilization!!
                com.cogoport.ares.api.settlement.entity.Document(
                    id = doc.id!!,
                    documentNo = doc.documentNo,
                    documentValue = doc.documentValue!!,
                    accountType = doc.accType.name,
                    documentAmount = doc.amountCurr,
                    organizationId = doc.organizationId!!,
                    documentType = doc.accType.name,
                    mappingId = doc.tradePartyMappingId,
                    dueDate = doc.dueDate,
                    taxableAmount = doc.taxableAmount!!,
                    afterTdsAmount = doc.amountCurr,
                    settledAmount = doc.payCurr,
                    balanceAmount = (doc.amountCurr - doc.payCurr),
                    currency = doc.currency,
                    ledCurrency = doc.ledCurrency,
                    settledTds = it.paidTds!!,
                    exchangeRate = it.exchangeRate,
                    signFlag = doc.signFlag,
                    approved = false,
                    accMode = doc.accMode,
                    documentDate = doc.transactionDate!!,
                    documentLedAmount = doc.amountLoc,
                    documentLedBalance = (doc.amountLoc - doc.payLoc),
                    sourceId = doc.documentNo,
                    sourceType = SettlementType.valueOf(doc.accType.name),
                    tdsCurrency = doc.currency,
                    tds = it.payableTds!!
                )
            }

            val documentModel = calculatingTds(documentEntity, sourceDocument.amount)

            val checkDocumentData = documentModel.map {
                CheckDocument(
                    id = it.id,
                    documentNo = it.documentNo,
                    documentValue = it.documentValue,
                    accountType = SettlementType.valueOf(it.accountType),
                    documentAmount = it.documentAmount,
                    tds = 0.toBigDecimal(),
                    afterTdsAmount = 0.toBigDecimal(),
                    balanceAmount = (it.balanceAmount),
                    accMode = it.accMode,
                    allocationAmount = it.allocationAmount!!,
                    currentBalance = it.currentBalance,
                    balanceAfterAllocation = it.balanceAfterAllocation!!,
                    ledgerAmount = it.ledgerAmount,
                    status = it.status.toString(),
                    currency = it.currency,
                    ledCurrency = it.ledCurrency,
                    exchangeRate = it.exchangeRate,
                    transactionDate = it.transactionDate,
                    settledTds = it.settledTds!!,
                    signFlag = it.signFlag,
                    nostroAmount = it.nostroAmount,
                    settledAmount = it.settledAmount,
                    settledAllocation = it.settledAllocation!!,
                    settledNostro = 0.toBigDecimal()
                )
            } as MutableList<CheckDocument>

            val checkRequest = CheckRequest(
                stackDetails = checkDocumentData,
                createdByUserType = null,
                incidentId = null,
                incidentMappingId = null,
                remark = null,
                createdBy = createdBy
            )
            settled = settlementServiceImpl.settle(checkRequest)
        }
        return settled
    }

    private suspend fun calculatingTds(documentEntity: List<com.cogoport.ares.api.settlement.entity.Document?>, settlingAmount: BigDecimal): List<com.cogoport.ares.model.settlement.Document> {
        val documentModel = settlementServiceImpl.groupDocumentList(documentEntity).map { documentConverter.convertToModel(it!!) }
        documentModel.forEach {
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.id = Hashids.encode(it.id.toLong())
        }
        for (doc in documentModel) {
            val totalTdsAmount = doc.tds + doc.settledTds!!
            doc.afterTdsAmount -= totalTdsAmount
            doc.balanceAmount -= totalTdsAmount
            doc.documentType = settlementServiceHelper.getDocumentType(AccountType.valueOf(doc.documentType), doc.signFlag, doc.accMode)
            doc.status = settlementServiceHelper.getDocumentStatus(
                docAmount = doc.documentAmount,
                balanceAmount = doc.currentBalance,
                docType = SettlementType.valueOf(doc.accountType)
            )
            doc.settledAllocation = BigDecimal.ZERO
            doc.allocationAmount = doc.balanceAmount
            doc.balanceAfterAllocation = BigDecimal.ZERO
        }

        documentModel[0].allocationAmount = settlingAmount
        documentModel[0].balanceAfterAllocation = documentModel[0].balanceAmount - settlingAmount
        return documentModel
    }
}
