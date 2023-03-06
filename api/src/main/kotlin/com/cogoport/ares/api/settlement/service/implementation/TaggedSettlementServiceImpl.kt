package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.AutoKnockoffDocumentResponse
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
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.util.UUID

@Singleton
open class TaggedSettlementServiceImpl : TaggedSettlementService {

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

    @Inject
    private lateinit var kuberMessagePublisher: KuberMessagePublisher

//    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun settleOnAccountInvoicePayment(req: OnAccountPaymentRequest) { // TODO(MULTIPLE SETTLEMENT WITH SAME SOURCE ID)
        val destinationDocument = AutoKnockoffDocumentResponse()
        var settledTaggedPreviousIds: List<Long>? = null
        val extendedSourceDocument = mutableListOf<AutoKnockoffDocumentResponse?>()
        val extendedTaggedSourceDocument = mutableListOf<AutoKnockoffDocumentResponse?>()
        req.document.documentNo = Hashids.decode(req.document.documentNo)[0].toString()
        val taggedDocumentIds = req.taggedDocuments.map { Hashids.decode(it.documentNo)[0] }
        destinationDocument.accountUtilization = accountUtilizationRepository.findRecord(req.document.documentNo.toLong(), AccountType.PINV.name, AccMode.AP.name)
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
            var paymentInfo = settledSourceDocuments.filter { it?.sourceId!!.toLong() == source.documentNo }
            paymentInfo = paymentInfo.sortedBy { it?.sourceType }
            settledTaggedPreviousIds = paymentInfo[0]?.taggedSettlementId?.split(",")?.toList()?.map { it.toLong() }
            val documentInfo = req.taggedDocuments.find { Hashids.decode(it.documentNo)[0] == paymentInfo[0]?.destinationId!!.toLong() }
            extendedSourceDocument.add(
                AutoKnockoffDocumentResponse(
                    accountUtilization = source,
                    settlementId = paymentInfo[0]?.settlementId,
                    paidTds = documentInfo?.paidTds,
                    payableTds = documentInfo?.payableTds,
                    exchangeRate = documentInfo?.exchangeRate!!,
                    taggedSettledIds = paymentInfo[0]?.taggedSettlementId?.split(",")?.toList()?.map { it.toLong() },
                    amount = if (!paymentInfo[0]?.isDraft!!) paymentInfo[0]?.amount!! else paymentInfo[0]?.unUtilizedAmount!!,
                    tdsAmount = if (paymentInfo.size == 2) { if (!paymentInfo[1]?.isDraft!!) paymentInfo[1]?.amount!! else paymentInfo[1]?.unUtilizedAmount!! } else BigDecimal.ZERO,
                    tdsSettlementId = if (paymentInfo.size == 2) { paymentInfo[1]?.settlementId!! } else null,
                )
            )
        }
        extendedSourceDocument.sortedByDescending { it?.accountUtilization?.amountCurr!! - it.accountUtilization?.payCurr!! }
        val distinctTaggedIds = settledTaggedPreviousIds?.distinct()
        if (distinctTaggedIds != null) {
            var previousSettlements = settlementRepository.findByIdIn(distinctTaggedIds)
            previousSettlements = previousSettlements.filter { it.unUtilizedAmount >= BigDecimal.ZERO }
            sourceType = previousSettlements.map { it.sourceType.toString() }.distinct()
            val taggedAccountUtilization = accountUtilizationRepository.findRecords(
                previousSettlements.map { it.sourceId!!.toLong() }, sourceType, null
            )
            previousSettlements.sortedByDescending { it.unUtilizedAmount }
            taggedAccountUtilization.forEach { source ->
                val document = previousSettlements.find { source.documentNo == it.sourceId }
                val tdsEntry = settlementRepository.findSettlement(document?.sourceId!!, document.destinationId, SettlementType.VTDS)
                extendedTaggedSourceDocument.add(
                    AutoKnockoffDocumentResponse(
                        accountUtilization = source,
                        amount = document.unUtilizedAmount,
                        payableTds = BigDecimal.ZERO,
                        paidTds = tdsEntry?.unUtilizedAmount ?: BigDecimal.ZERO,
                        tdsAmount = tdsEntry?.unUtilizedAmount ?: BigDecimal.ZERO,
                        taggedSettledIds = document.taggedSettlementId?.split(",")?.toList()?.map { it.toLong() },
                        exchangeRate = BigDecimal.ONE,
                        settlementId = document.id,
                        tdsSettlementId = tdsEntry?.id
                    )
                )
            }
        }

        var balanceSettlingAmount = destinationDocument.accountUtilization?.amountCurr!!.minus(destinationDocument.accountUtilization?.payCurr!!)
        var sourceStartIndex = 0
        var taggedSourceStartIndex = 0
        val taggedSourceEndIndex = extendedTaggedSourceDocument.size
        val sourceEndIndex = extendedSourceDocument.size
        while (sourceStartIndex < sourceEndIndex && balanceSettlingAmount > BigDecimal.ZERO) {
            val source = extendedSourceDocument[sourceStartIndex]
            if (req.settledAmount + req.settledTds >= BigDecimal.ZERO) {
                if (source?.amount!! > BigDecimal.ZERO) {
                    balanceSettlingAmount = doSettlement(destinationDocument, source, req, balanceSettlingAmount)
                }
            }
            sourceStartIndex++
        }

        while (taggedSourceStartIndex < taggedSourceEndIndex && balanceSettlingAmount > BigDecimal.ZERO) {
            val source = extendedTaggedSourceDocument[taggedSourceStartIndex]
            if (req.settledAmount + req.settledTds >= BigDecimal.ZERO) {
                if (source?.amount!! > BigDecimal.ZERO) {
                    balanceSettlingAmount = doSettlement(destinationDocument, source, req, balanceSettlingAmount)
                }
            }
            taggedSourceStartIndex++
        }
    }

    private suspend fun doSettlement(
        destinationDocument: AutoKnockoffDocumentResponse,
        sourceDocument: AutoKnockoffDocumentResponse?,
        req: OnAccountPaymentRequest,
        balanceSettlingAmount: BigDecimal
    ): BigDecimal {
        sourceDocument?.amount = minOf((sourceDocument?.amount!! - sourceDocument.tdsAmount), req.settledAmount, (balanceSettlingAmount - req.document.payableTds))
        sourceDocument.paidTds = minOf(sourceDocument.paidTds!!, req.settledTds, sourceDocument.tdsAmount) // TODO(add tds entry amount too)
        val settledList = settleTaggedDocument(destinationDocument, sourceDocument, req.createdBy)
        val taggedIds = mutableListOf<Long>()
        if (sourceDocument.taggedSettledIds.isNullOrEmpty()) {
            taggedIds.add(sourceDocument.settlementId!!)
        } else {
            taggedIds.addAll(sourceDocument.taggedSettledIds!!)
            taggedIds.add(sourceDocument.settlementId!!)
        } // testing

        var settlement = settlementRepository.getSettlementDetailsByDestinationId(destinationDocument.accountUtilization!!.documentNo, sourceDocument.accountUtilization?.documentNo!!)
        if (settlement.isNotEmpty()) {
            val tdsSettled = settlement.find { it.sourceType == SettlementType.VTDS }
            if (tdsSettled == null && settlement.size > 1) {
                settlement.removeLast()
            }
            settlement = settlement.sortedBy { it.sourceType }.toMutableList()
            val amountPaid = settlement[0].amount!!
            val tdsAmount = tdsSettled?.amount ?: BigDecimal.ZERO
            val totalAmountPaid = amountPaid + tdsAmount!!
            settlementRepository.updateTaggedSettlement(sourceDocument.settlementId!!, amountPaid)
            if (sourceDocument.tdsSettlementId != null) {
                settlementRepository.updateTaggedSettlement(sourceDocument.tdsSettlementId!!, tdsAmount)
            }
            settlementRepository.updateTaggedSettlementIds(settlement.map { it.id!! }, taggedIds.joinToString())
            destinationDocument.accountUtilization!!.payCurr += totalAmountPaid
            destinationDocument.accountUtilization!!.payLoc += (totalAmountPaid * settledList!![1].exchangeRate)
            destinationDocument.payableTds = destinationDocument.payableTds!! - tdsAmount
            destinationDocument.paidTds = destinationDocument.paidTds!! + tdsAmount
            req.settledAmount -= amountPaid
            req.settledTds -= tdsAmount
        }
        return destinationDocument.accountUtilization!!.amountCurr - destinationDocument.accountUtilization!!.payCurr
    }
    private suspend fun reversePayment(reversePaymentRequest: ReversePaymentRequest) {
        val accountUtilization = accountUtilizationRepository.findRecords(listOf(reversePaymentRequest.document, reversePaymentRequest.source), listOf(AccountType.PINV.name, AccountType.PAY.name, AccountType.PCN.name), AccMode.AP.name)
        val settlements = settlementRepository.getSettlementDetailsByDestinationId(reversePaymentRequest.document, reversePaymentRequest.source)
        val document = accountUtilization.find { it.accType == AccountType.PINV }
        val source = accountUtilization.find { it.accType != AccountType.PINV }
        val amount = settlements.sumOf {
            when (it.sourceType) {
                SettlementType.PCN, SettlementType.PAY -> it.amount ?: BigDecimal.ZERO
                else -> BigDecimal.ZERO
            }
        }

        val tsdAmount = settlements.sumOf {
            when (it.sourceType) {
                SettlementType.VTDS -> it.amount ?: BigDecimal.ZERO
                else -> BigDecimal.ZERO
            }
        }
        settlements.map {
            it.unUtilizedAmount = it.amount!!
            it.isDraft = true
        }
        settlementRepository.updateAll(settlements)
        accountUtilizationRepository.markPaymentUnutilized(source?.id!!, amount + tsdAmount, ((amount + tsdAmount) * reversePaymentRequest.exchangeRate))
        accountUtilizationRepository.updateAccountUtilizations(document?.id!!, true)

        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, source.id, AresConstants.UPDATE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, document.id!!, AresConstants.DRAFT, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)

//        try {
//            kuber
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
} // TODO(remove kuber lib file and from build gradle)
