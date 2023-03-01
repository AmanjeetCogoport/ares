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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

        val settledSourceDocuments = settlementRepository.getPaymentsCorrespondingDocumentNo(taggedDocumentIds)
        settledSourceDocuments.forEach { it1 ->
            val exchangeRate = req.taggedDocuments.find { it2 -> Hashids.decode(it2.documentNo)[0] == it1?.destinationId!!.toLong() }!!.exchangeRate
            if (!it1?.isDraft!!) {
                if (listOf(SettlementType.PCN, SettlementType.PAY, SettlementType.PREIMB).contains(it1.sourceType)) {
                    reversePayment(
                        ReversePaymentRequest(
                            it1.destinationId.toLong(),
                            it1.sourceId.toLong(),
                            exchangeRate,
                            req.createdBy,
                            null
                        )
                    )
                }
            }
        }
        var sourceType: List<String?> = settledSourceDocuments.map { it?.sourceType.toString() }.distinct()
        val sourceAccountUtilization = accountUtilizationRepository.findRecords(
            settledSourceDocuments.map { it?.sourceId!!.toLong() }, sourceType, null
        )

        sourceAccountUtilization.forEach { source ->
            var paymentInfo = settledSourceDocuments.filter { it?.sourceId!!.toLong() == source?.documentNo }
            paymentInfo = paymentInfo.sortedBy { it?.sourceType }
            val documentInfo = req.taggedDocuments.find { Hashids.decode(it.documentNo)[0] == paymentInfo[0]?.destinationId!!.toLong() }
            val isSettled = !(paymentInfo[0]?.transRefNumber == documentInfo?.transactionNo && paymentInfo[0]?.sourceType == SettlementType.PAY)

            val taggedSettledIds = if (!paymentInfo[0]?.taggedSettlementId.isNullOrEmpty()) ObjectMapper().readValue<HashMap<String?, ArrayList<Long?>>?>(paymentInfo[0]?.taggedSettlementId!!) else hashMapOf("taggedIds" to arrayListOf())
            if (!taggedSettledIds.isNullOrEmpty()) {
                settledTaggedPreviousIds.addAll(ArrayList(taggedSettledIds["taggedIds"]))
            }
            extendedSourceDocument.add(
                AutoKnockoffDocumentResponse(
                    accountUtilization = source,
                    settlementId = paymentInfo[0]?.settlementId,
                    paidTds = documentInfo?.paidTds,
                    payableTds = documentInfo?.payableTds,
                    exchangeRate = documentInfo?.exchangeRate!!,
                    isSettled = isSettled,
                    taggedSettledIds = taggedSettledIds?.get("taggedIds"),
                    amount = if (!paymentInfo[0]?.isDraft!!) paymentInfo[0]?.amount!! else paymentInfo[0]?.unUtilizedAmount!!,
                    tdsAmount = if (paymentInfo.size == 2) { if (!paymentInfo[1]?.isDraft!!) paymentInfo[1]?.amount!! else paymentInfo[1]?.unUtilizedAmount!! } else BigDecimal.ZERO,
                    tdsSettlementId = if (paymentInfo.size == 2) { paymentInfo[1]?.settlementId!! } else null,
                )
            )
        }
        extendedSourceDocument.sortedByDescending { it?.accountUtilization?.amountCurr!! - it.accountUtilization?.payCurr!! }
        val distinctTaggedIds = settledTaggedPreviousIds.distinct()
        var previousSettlements = listOf<Settlement>()
        if (distinctTaggedIds.isNotEmpty()) {
            previousSettlements = settlementRepository.findByIdIn(distinctTaggedIds as List<Long>)
            previousSettlements = previousSettlements.filter { it.unUtilizedAmount >= BigDecimal.ZERO }
            sourceType = previousSettlements.map { it.sourceType.toString() }.distinct()
            val taggedAccountUtilization = accountUtilizationRepository.findRecords(
                previousSettlements.map { it.sourceId!!.toLong() }, sourceType, null
            )
            previousSettlements.sortedByDescending { it.unUtilizedAmount }
            taggedAccountUtilization.forEach { source ->
                val document = previousSettlements.find { source?.documentNo == it.sourceId }
                val taggedSettledIds = if (document?.taggedSettlementId != null) ObjectMapper().readValue<HashMap<String?, ArrayList<Long?>>?>(document.taggedSettlementId!!) else hashMapOf("taggedIds" to arrayListOf<Long?>())
                val tdsEntry = settlementRepository.findBySourceId(document?.sourceId!!, document.destinationId, SettlementType.VTDS)
                extendedTaggedSourceDocument.add(
                    AutoKnockoffDocumentResponse(
                        accountUtilization = source,
                        amount = document.unUtilizedAmount,
                        payableTds = BigDecimal.ZERO,
                        paidTds = tdsEntry?.unUtilizedAmount ?: BigDecimal.ZERO,
                        tdsAmount = tdsEntry?.unUtilizedAmount ?: BigDecimal.ZERO,
                        taggedSettledIds = taggedSettledIds?.get("taggedIds")!!,
                        exchangeRate = BigDecimal.ONE
                    )
                )
            }
        }

        var balanceSettlingAmount = destinationDocument.accountUtilization?.amountCurr!!.minus(destinationDocument.accountUtilization?.payCurr!!)
        var sourceStartIndex = 0
        var taggedSourceStartIndex = 0
        val taggedSourceEndIndex = previousSettlements.size
        val sourceEndIndex = extendedSourceDocument.size
        while (sourceStartIndex < sourceEndIndex) {
            val source = extendedSourceDocument[sourceStartIndex]
            if (req.settledAmount + req.settledTds >= BigDecimal.ZERO && balanceSettlingAmount > BigDecimal.ZERO) {
                if (source?.amount!! > BigDecimal.ZERO) {
                    balanceSettlingAmount = doSettlement(destinationDocument, source, req, balanceSettlingAmount)
                }
            }
            sourceStartIndex++
        }

        while (taggedSourceStartIndex < taggedSourceEndIndex) {
            val source = extendedTaggedSourceDocument[taggedSourceStartIndex]
            if (req.settledAmount + req.settledTds >= BigDecimal.ZERO && balanceSettlingAmount > BigDecimal.ZERO) {
                if (source?.amount!! > BigDecimal.ZERO) {
                    balanceSettlingAmount = doSettlement(destinationDocument, source, req, balanceSettlingAmount)
                }
                taggedSourceStartIndex++
            }
        }
    }

    private suspend fun doSettlement(
        destinationDocument: AutoKnockoffDocumentResponse,
        sourceDocument: AutoKnockoffDocumentResponse?,
        req: OnAccountPaymentRequest,
        balanceSettlingAmount: BigDecimal
    ): BigDecimal {
        sourceDocument?.amount = minOf((sourceDocument?.amount!! - sourceDocument.tdsAmount), req.settledAmount, (balanceSettlingAmount - req.document.payableTds))
        sourceDocument.paidTds = minOf(sourceDocument.paidTds!!, sourceDocument.tdsAmount) // TODO(add tds entry amount too)
        val settledList = settleTaggedDocument(destinationDocument, sourceDocument, req.createdBy)

        if (sourceDocument.taggedSettledIds.isNullOrEmpty()) {
            sourceDocument.taggedSettledIds = mutableListOf(sourceDocument.settlementId)
        } else {
            sourceDocument.taggedSettledIds?.add(sourceDocument.settlementId)
        }
        var balanceAmount = balanceSettlingAmount
        var settlement = settlementRepository.getSettlementDetailsByDestinationId(destinationDocument.accountUtilization!!.documentNo, sourceDocument.accountUtilization?.documentNo!!)
        if (!settlement.isNullOrEmpty()) {
            settlement = settlement.sortedBy { it.sourceType }
            val amountPaid = settlement[0].amount!!
            val tdsAmount = if (settlement.size == 2) settlement[1].amount else BigDecimal.ZERO
            val totalAmountPaid = amountPaid + tdsAmount!!

            settlementRepository.updateTaggedSettlement(sourceDocument.settlementId!!, sourceDocument.amount)
            if (sourceDocument.tdsSettlementId != null) {
                settlementRepository.updateTaggedSettlement(sourceDocument.tdsSettlementId!!, sourceDocument.paidTds!!)
            }
            settlementRepository.updateTaggedSettlementAmount(settlement.map { it.id!! }, ArrayList(sourceDocument.taggedSettledIds),)
            destinationDocument.accountUtilization!!.payCurr += totalAmountPaid
            destinationDocument.accountUtilization!!.payLoc += (totalAmountPaid * settledList!![1].exchangeRate)
            destinationDocument.payableTds = destinationDocument.payableTds!! - tdsAmount
            destinationDocument.paidTds = destinationDocument.paidTds!! + tdsAmount
            balanceAmount = destinationDocument.accountUtilization!!.amountCurr - destinationDocument.accountUtilization!!.payCurr
            req.settledAmount -= amountPaid
            req.settledTds -= tdsAmount
        }
        return balanceAmount
    }
    private suspend fun reversePayment(reversePaymentRequest: ReversePaymentRequest) {
        val accountUtilization = accountUtilizationRepository.findRecord(reversePaymentRequest.document, AccountType.PINV.name, AccMode.AP.name)
        var settlements = settlementRepository.getSettlementDetailsByDestinationId(accountUtilization!!.documentNo, reversePaymentRequest.source)
        settlements = settlements.sortedBy { it.sourceType }
        val amount = settlements[0].amount!!
        val settlementIds = settlements.map { it.id!! }
        settlements.forEach {
            settlementRepository.markSettlementIsDraftTrue(it.id!!, it.amount!!)
        }
        val accountUtilizationPaymentData = accountUtilizationRepository.getDataByPaymentNum(reversePaymentRequest.source)
        accountUtilizationRepository.markPaymentUnutilized(accountUtilizationPaymentData.id, amount, (amount * reversePaymentRequest.exchangeRate))
        accountUtilizationRepository.updateAccountUtilizations(accountUtilization.id!!, true)
        createAudit(AresConstants.SETTLEMENT, settlementIds[0], AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        if (settlementIds.size == 2) {
            createAudit(AresConstants.SETTLEMENT, settlementIds[1], AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        }
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
                    tds = it.tds,
                    afterTdsAmount = it.afterTdsAmount,
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
            settled = settlementServiceImpl.settle(checkRequest, true)
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
        documentModel[1].tds = minOf(documentModel[0].settledTds!!, documentModel[1].tds)
        documentModel[0].allocationAmount = settlingAmount
        documentModel[0].balanceAfterAllocation = documentModel[0].balanceAmount - settlingAmount
        documentModel[1].allocationAmount = settlingAmount
        documentModel[1].balanceAfterAllocation = documentModel[1].balanceAmount - settlingAmount
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
