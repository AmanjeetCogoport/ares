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
            extendedSourceDocument.add(
                AutoKnockoffDocumentResponse(
                    accountUtilization = source,
                    paidTds = documentInfo?.paidTds,
                    payableTds = documentInfo?.payableTds,
                    exchangeRate = documentInfo?.exchangeRate!!
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
        var balanceSettlingAmount = destinationDocument.accountUtilization?.amountCurr!! - destinationDocument.accountUtilization?.payCurr!!
        var sourceStartIndex = 0
        val sourceEndIndex = extendedSourceDocument.size
        while (balanceSettlingAmount.compareTo(BigDecimal.ZERO) > 0 && sourceStartIndex < sourceEndIndex) {
            var settledList: List<CheckDocument>?
            val source = extendedSourceDocument[sourceStartIndex]
            val balancePaymentUtilization = source!!.accountUtilization?.amountCurr!! - source.accountUtilization?.payCurr!!
            if ((balancePaymentUtilization != BigDecimal.ZERO) && (balancePaymentUtilization >= balanceSettlingAmount)) {
                settleTaggedDocument(destinationDocument, source, req.createdBy)
            }
            if ((balanceSettlingAmount > BigDecimal.ZERO) && (source.accountUtilization?.payCurr!! < source.accountUtilization!!.amountCurr)) {
                settledList = settleTaggedDocument(destinationDocument, source, req.createdBy)
            } else {
                val payment =
                    settledSourceDocuments.find { it?.sourceId!!.toLong() == source.accountUtilization?.documentNo }
                val availablePayment = reversePayment(
                    ReversePaymentRequest(
                        payment?.destinationId!!.toLong(),
                        payment.sourceId.toLong(),
                        req.createdBy,
                        null
                    )
                )
                source.accountUtilization!!.payCurr = availablePayment.payCurr
                source.accountUtilization!!.payLoc = availablePayment.payLoc
                settledList = settleTaggedDocument(destinationDocument, source, req.createdBy)
                destinationDocument.accountUtilization!!.payCurr = settledList?.get(1)?.settledAmount!!
                destinationDocument.accountUtilization!!.payLoc = settledList[1].settledAmount!! * settledList[1].exchangeRate
            }
            balanceSettlingAmount = settledList?.get(1)?.balanceAmount!!
            sourceStartIndex++
        }
    }
    private suspend fun reversePayment(reversePaymentRequest: ReversePaymentRequest): PaymentUtilizationResponse {
        val accountUtilization = accountUtilizationRepository.findRecord(reversePaymentRequest.document, AccountType.PINV.name, AccMode.AP.name)
        val settlementIds = settlementRepository.getSettlementByDestinationId(accountUtilization!!.documentNo, reversePaymentRequest.source)
        settlementRepository.markSettlementIsDraftTrue(settlementIds) // TODO("Don't Delete The Settlement, Mark it as Draft, LEFT all the table same")

        val accountUtilizationPaymentData = accountUtilizationRepository.getDataByPaymentNum(reversePaymentRequest.source)
        accountUtilizationRepository.markPaymentUnutilized(accountUtilizationPaymentData.id, BigDecimal.ZERO, BigDecimal.ZERO) // TODO("nothing")
        accountUtilizationRepository.updateAccountUtilizations(accountUtilization.id!!, true)
        accountUtilizationPaymentData.payCurr = BigDecimal.ZERO
        accountUtilizationPaymentData.payLoc = BigDecimal.ZERO
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

            val documentModel = calculatingTds(documentEntity)

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

    private suspend fun calculatingTds(documentEntity: List<com.cogoport.ares.api.settlement.entity.Document?>): List<com.cogoport.ares.model.settlement.Document> {
        val documentModel = settlementServiceImpl.groupDocumentList(documentEntity).map { documentConverter.convertToModel(it!!) }
        documentModel.forEach {
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.id = Hashids.encode(it.id.toLong())
        }
        for (doc in documentModel) {
            doc.afterTdsAmount -= (doc.tds + doc.settledTds!!)
            doc.balanceAmount -= doc.tds
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

        return documentModel
    }
}
