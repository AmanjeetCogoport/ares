package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.AutoKnockoffDocumentResponse
import com.cogoport.ares.api.payment.model.ReversePaymentRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.Settlement
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
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
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
        val settledTaggedPreviousIds = arrayListOf<Long?>()
        val extendedSourceDocument = mutableListOf<AutoKnockoffDocumentResponse?>()
        val extendedTaggedSourceDocument = mutableListOf<AutoKnockoffDocumentResponse?>()
        req.document.documentNo = Hashids.decode(req.document.documentNo)[0].toString()
        val taggedDocumentIds = req.taggedDocuments.map { Hashids.decode(it.documentNo)[0] }
        destinationDocument.accountUtilization = accountUtilizationRepository.findRecord(
            req.document.documentNo.toLong(),
            AccountType.PINV.name,
            AccMode.AP.name
        )
        if (destinationDocument.accountUtilization == null) {
            throw AresException(AresError.ERR_1503, "")
        }
        destinationDocument.payableTds = req.document.payableTds
        destinationDocument.paidTds = req.document.paidTds
        destinationDocument.exchangeRate = req.document.exchangeRate

        // TODO("only have to use amount of pcn or pay which is settled")
        val settledSourceDocuments = settlementRepository.getPaymentsCorrespondingDocumentNo(taggedDocumentIds)
        settledSourceDocuments.forEach { it1 ->
            val exchangeRate = req.taggedDocuments.find { it2 -> Hashids.decode(it2.documentNo)[0] == it1?.destinationId!!.toLong() }!!.exchangeRate
            reversePayment(
                ReversePaymentRequest(
                    it1!!.destinationId.toLong(),
                    it1.sourceId.toLong(),
                    exchangeRate,
                    req.createdBy,
                    null
                )
            )
        }
        var sourceType: List<String?> = settledSourceDocuments.map { it?.sourceType.toString() }.distinct()
        val sourceAccountUtilization = accountUtilizationRepository.findRecords(
            settledSourceDocuments.map { it?.sourceId!!.toLong() }, sourceType, null
        )

        sourceAccountUtilization.forEach { source ->
            val paymentInfo = settledSourceDocuments.find { it?.sourceId!!.toLong() == source?.documentNo }
            val documentInfo = req.taggedDocuments.find { Hashids.decode(it.documentNo)[0] == paymentInfo?.destinationId!!.toLong() }
            val isSettled = !(paymentInfo?.transRefNumber == documentInfo?.transactionNo && paymentInfo?.sourceType == SettlementType.PAY)
            val taggedSettledIds = ObjectMapper().readValue(paymentInfo?.utilizedAmount, object : TypeReference<MutableList<Long?>>() {})
            settledTaggedPreviousIds.addAll(ArrayList(taggedSettledIds))
            extendedSourceDocument.add(
                AutoKnockoffDocumentResponse(
                    accountUtilization = source,
                    settlementId = paymentInfo?.settlementId,
                    paidTds = documentInfo?.paidTds,
                    payableTds = documentInfo?.payableTds,
                    exchangeRate = documentInfo?.exchangeRate!!,
                    isSettled = isSettled,
                    amount = paymentInfo?.amount!!,
                    taggedSettledIds = taggedSettledIds
                )
            )
        }
        extendedSourceDocument.sortedByDescending { it?.accountUtilization?.amountCurr!! - it.accountUtilization?.payCurr!! }
        val distinctTaggedIds = settledTaggedPreviousIds.distinct()
        var previousSettlements = listOf<Settlement>()
        if (distinctTaggedIds.isNotEmpty()) {
            previousSettlements = settlementRepository.findByIdIn(distinctTaggedIds as List<Long>)
            previousSettlements = previousSettlements.filter { it.unUtilizedAmount >= BigDecimal.ZERO }
        }
        sourceType = previousSettlements.map { it.sourceType.toString() }.distinct()
        val taggedAccountUtilization = accountUtilizationRepository.findRecords(
            previousSettlements.map { it.sourceId!!.toLong() }, sourceType, null
        )
        previousSettlements.sortedByDescending { it.unUtilizedAmount }

        taggedAccountUtilization.forEach { source ->
            extendedTaggedSourceDocument.add(
                AutoKnockoffDocumentResponse(
                    accountUtilization = source
                )
            )
        }

        var balanceSettlingAmount = destinationDocument.accountUtilization?.amountCurr!!.minus(destinationDocument.accountUtilization?.payCurr!!)
        var sourceStartIndex = 0
        var taggedSourceStartIndex = 0
        val taggedSourceEndIndex = previousSettlements.size
        val sourceEndIndex = extendedSourceDocument.size
        while (req.settledAmount + req.settledTds >= BigDecimal.ZERO && balanceSettlingAmount > BigDecimal.ZERO && (sourceStartIndex < sourceEndIndex || taggedSourceStartIndex < taggedSourceEndIndex)) {
            val source = if (sourceStartIndex < sourceEndIndex) extendedSourceDocument[sourceStartIndex] else extendedTaggedSourceDocument[taggedSourceEndIndex]
            source?.amount = minOf(source?.amount!!, req.settledAmount, balanceSettlingAmount)
            val settledList = settleTaggedDocument(destinationDocument, source, req.createdBy)
            destinationDocument.accountUtilization!!.payCurr += source.amount
            destinationDocument.accountUtilization!!.payLoc += (source.amount * settledList!![1].exchangeRate)
            destinationDocument.payableTds = destinationDocument.payableTds!! - settledList[1].settledTds
            destinationDocument.payableTds = destinationDocument.paidTds!! + settledList[1].settledTds
            source.paidTds = source.paidTds!! - settledList[1].settledTds
            settlementRepository.updateTaggedSettlement(source.settlementId!!, source.amount)
            balanceSettlingAmount = destinationDocument.accountUtilization!!.amountCurr - destinationDocument.accountUtilization!!.payCurr // TODO(wrong balanceAmount)
            req.settledAmount -= settledList[1].settledAllocation
            req.settledTds -= settledList[1].settledTds // TODO(wrong settledTds)
            if (sourceStartIndex < sourceEndIndex) sourceStartIndex++ else taggedSourceStartIndex++
        }
    }
    private suspend fun reversePayment(reversePaymentRequest: ReversePaymentRequest) {
        val accountUtilization = accountUtilizationRepository.findRecord(reversePaymentRequest.document, AccountType.PINV.name, AccMode.AP.name)
        val settlements = settlementRepository.getSettlementDetailsByDestinationId(accountUtilization!!.documentNo, reversePaymentRequest.source)
        val amount = settlements.sumOf { it.amount!! }
        val settlementIds = settlements.map { it.id!! }
        settlementRepository.markSettlementIsDraftTrue(settlementIds, amount)
        val accountUtilizationPaymentData = accountUtilizationRepository.getDataByPaymentNum(reversePaymentRequest.source)
        accountUtilizationRepository.markPaymentUnutilized(accountUtilizationPaymentData.id, amount, amount * reversePaymentRequest.exchangeRate)
        accountUtilizationRepository.updateAccountUtilizations(accountUtilization.id!!, true)
        createAudit(AresConstants.SETTLEMENT, settlementIds[0], AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.SETTLEMENT, settlementIds[1], AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilizationPaymentData.id, AresConstants.UPDATE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilization.id!!, AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)

//        try {
//            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accountUtilization.organizationId))
//        } catch (e: Exception) {
//            Sentry.captureException(e)
//        }
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
}
