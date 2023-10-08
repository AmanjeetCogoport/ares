package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.AutoKnockoffDocumentResponse
import com.cogoport.ares.api.payment.model.ReversePaymentRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.Document
import com.cogoport.ares.api.settlement.entity.SettlementTaggedMapping
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.model.TaggedInvoiceSettlementInfo
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.repository.SettlementTaggedMappingRepository
import com.cogoport.ares.api.settlement.service.interfaces.TaggedSettlementService
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.brahma.hashids.Hashids
import io.sentry.Sentry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.util.UUID
import javax.transaction.Transactional

@Singleton
open class TaggedSettlementServiceImpl : TaggedSettlementService {

    @Inject
    private lateinit var settlementRepository: SettlementRepository

    @Inject
    private lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    private lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Inject
    private lateinit var auditService: AuditService

    @Inject
    private lateinit var settlementServiceHelper: SettlementServiceHelper

    @Inject
    private lateinit var settlementServiceImpl: SettlementServiceImpl

    @Inject
    private lateinit var settlementTaggedMappingRepository: SettlementTaggedMappingRepository

    @Inject
    private lateinit var documentConverter: DocumentMapper

    @Inject
    private lateinit var aresMessagePublisher: AresMessagePublisher

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun settleOnAccountInvoicePayment(req: OnAccountPaymentRequest) {
        val destinationDocument = AutoKnockoffDocumentResponse()
        val settlementIds = mutableSetOf<Long?>()
        val taggedBillIds = mutableListOf<Long>()
        val extendedSourceDocument = mutableListOf<AutoKnockoffDocumentResponse?>()

        destinationDocument.accountUtilization = accountUtilizationRepository.findRecord(req.document, AccountType.PINV.name, AccMode.AP.name)

        if (destinationDocument.accountUtilization == null) {
            throw AresException(AresError.ERR_1503, "")
        }
        destinationDocument.exchangeRate = destinationDocument.accountUtilization?.amountLoc!!.divide(destinationDocument.accountUtilization?.amountCurr, 4, RoundingMode.HALF_UP)?: BigDecimal.ONE

        val settledSourceDocuments = settlementRepository.getPaymentsCorrespondingDocumentNo(req.taggedDocuments!!)
        if (!req.taggedDocuments.isNullOrEmpty()) {
            accountUtilizationRepo.updateAccountUtilizations(req.taggedDocuments!!, true)
            if (settledSourceDocuments.isEmpty()) {
                try {
                    aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = destinationDocument.accountUtilization?.organizationId))
                } catch (e: Exception) {
                    Sentry.captureException(e)
                }
                return
            }
        }

        settledSourceDocuments.forEach { it1 ->
            settlementIds.add(it1?.settlementId)
            if (it1?.taggedBillId != null) {
                taggedBillIds.addAll(it1.taggedBillId.split(", ").toList().map { it.toLong() })
            }
            if (!it1?.isVoid!!) {
                if (listOf(SettlementType.PCN, SettlementType.PAY).contains(it1.sourceType)) {
                    reversePayment(
                        ReversePaymentRequest(
                            it1.destinationId.toLong(),
                            it1.sourceId.toLong(),
                            req.createdBy,
                            null
                        )
                    )
                }
            }
        }

        val sourceType: List<String?> = settledSourceDocuments.map { it?.sourceType.toString() }.distinct()
        val sourceAccountUtilization = accountUtilizationRepo.findRecords(settledSourceDocuments.map { it?.sourceId!!.toLong() }, sourceType, null)

        val settlementTaggedMapping = settlementTaggedMappingRepository.getAllSettlementIds(settlementIds.toList())
        val settleMappingIds = mutableSetOf<Long>()
        settleMappingIds.addAll(settlementTaggedMapping.map { it.settlementId })
        settleMappingIds.addAll(settlementTaggedMapping.map { it.utilizedSettlementId })

        val settled = settlementRepository.findByIdInOrderByAmountDesc(settleMappingIds.toList())
        var taggedSettlements: MutableList<TaggedInvoiceSettlementInfo?> = mutableListOf()
        if (taggedBillIds.isNotEmpty()) {
            taggedSettlements = settlementRepository.getPaymentsCorrespondingDocumentNo(taggedBillIds.distinct())
            val sourceAcc = accountUtilizationRepo.findRecords(taggedSettlements.map { it?.sourceId!!.toLong() }, listOf("PAY", "PCN"), "AP")
            sourceAccountUtilization.addAll(sourceAcc.filter { it !in sourceAccountUtilization })
        }

        sourceAccountUtilization.forEach { source ->
            var paymentInfo = settledSourceDocuments.filter { it?.sourceId!!.toLong() == source.documentNo }
            if (paymentInfo.isNotEmpty()) {
                paymentInfo = paymentInfo.sortedBy { it?.sourceType }
                val amount = if (paymentInfo[0]?.amount!! > (destinationDocument.accountUtilization!!.amountCurr - destinationDocument.accountUtilization!!.tdsAmount!! - destinationDocument.accountUtilization!!.payCurr) && !paymentInfo[0]?.isVoid!!) {
                    paymentInfo[0]?.amount!!
                } else {
                    val doc = settled?.find { it.sourceId == source.documentNo && it.isVoid!! }
                    if (doc == null) {
                        paymentInfo[0]?.amount
                    } else {
                        val maxAmountBorrowed = settled.filter { it.sourceId == source.documentNo && !it.isVoid!! }
                            .sumOf { it.amount ?: BigDecimal.ZERO }
                        doc.amount?.minus(maxAmountBorrowed)
                    }
                }
                extendedSourceDocument.add(
                    AutoKnockoffDocumentResponse(
                        accountUtilization = source,
                        settlementId = paymentInfo[0]?.settlementId,
                        exchangeRate = source.amountLoc.divide(source.amountCurr),
                        destinationId = paymentInfo[0]?.destinationId!!.toLong(),
                        taggedSettledIds = paymentInfo[0]?.taggedBillId?.split(", ")?.toList()?.map { it.toLong() },
                        amount = amount ?: BigDecimal.ZERO
                    )
                )
            } else {
                val settleInfo = taggedSettlements.filter { it?.sourceId?.toLong() == source.documentNo }
                extendedSourceDocument.add(
                    AutoKnockoffDocumentResponse(
                        accountUtilization = source,
                        settlementId = settled?.get(0)?.id,
                        exchangeRate = source.amountLoc.divide(source.amountCurr),
                        destinationId = settleInfo[0]?.destinationId!!.toLong(),
                        taggedSettledIds = settleInfo[0]?.taggedBillId?.split(", ")?.toList()?.map { it.toLong() },
                        amount = source.amountCurr - source.tdsAmount!! - source.payCurr
                    )
                )
            }
        }

        var balanceSettlingAmount = destinationDocument.accountUtilization!!.amountCurr.minus(destinationDocument.accountUtilization!!.tdsAmount!!).minus(destinationDocument.accountUtilization!!.payCurr)
        var sourceStartIndex = 0
        val sourceEndIndex = extendedSourceDocument.size
        while (sourceStartIndex < sourceEndIndex && balanceSettlingAmount > BigDecimal.ZERO) {
            val source = extendedSourceDocument[sourceStartIndex]
            if (source?.amount!! > BigDecimal.ZERO) {
                balanceSettlingAmount = doSettlement(destinationDocument, source, req, balanceSettlingAmount)
            }
            sourceStartIndex++
        }
    }

    private suspend fun doSettlement(
        destinationDocument: AutoKnockoffDocumentResponse,
        sourceDocument: AutoKnockoffDocumentResponse?,
        req: OnAccountPaymentRequest,
        balanceSettlingAmount: BigDecimal
    ): BigDecimal {
        val sourceUnUtilizedAmount = sourceDocument?.accountUtilization!!.amountCurr - sourceDocument.accountUtilization!!.tdsAmount!! - sourceDocument.accountUtilization!!.payCurr
        sourceDocument.amount = minOf(sourceDocument.amount, balanceSettlingAmount, sourceUnUtilizedAmount)
        val settledList = settleTaggedDocument(destinationDocument, sourceDocument, req.createdBy)
        val taggedIds = mutableListOf<Long>()
        if (sourceDocument.taggedSettledIds.isNullOrEmpty()) {
            taggedIds.addAll(req.taggedDocuments!!)
        } else {
            taggedIds.addAll(sourceDocument.taggedSettledIds!!)
            taggedIds.addAll(req.taggedDocuments!!)
        }

        val settlement = settlementRepository.getSettlementDetailsByDestinationId(destinationDocument.accountUtilization!!.documentNo, sourceDocument.accountUtilization?.documentNo!!)
        if (settlement != null) {
            accountUtilizationRepo.updateTaggedBillIds(destinationDocument.accountUtilization!!.documentNo, taggedIds.joinToString())
            settlementTaggedMappingRepository.save(SettlementTaggedMapping(id = null, settlementId = settlement.id!!, utilizedSettlementId = sourceDocument.settlementId!!))
            destinationDocument.accountUtilization!!.payCurr += settlement.amount!!
            destinationDocument.accountUtilization!!.payLoc += (settlement.amount!! * settledList!![1].exchangeRate)
        }
        return (destinationDocument.accountUtilization!!.amountCurr - destinationDocument.accountUtilization!!.tdsAmount!! - destinationDocument.accountUtilization!!.payCurr)
    }
    private suspend fun reversePayment(reversePaymentRequest: ReversePaymentRequest) {
        val accountUtilization = accountUtilizationRepo.findRecords(listOf(reversePaymentRequest.document, reversePaymentRequest.source), listOf(AccountType.PINV.name, AccountType.PAY.name, AccountType.PCN.name), AccMode.AP.name)
        val settlement = settlementRepository.getSettlementDetailsByDestinationId(reversePaymentRequest.document, reversePaymentRequest.source)
            ?: throw AresException(AresError.ERR_1000, "Settlement Not Found")
        val document = accountUtilization.find { it.accType == AccountType.PINV }
        val source = accountUtilization.find { it.accType != AccountType.PINV }
        settlementRepository.updateVoidStatus(settlement.id!!, true)
        accountUtilizationRepo.markPaymentUnutilized(source?.id!!, settlement.amount!!, (settlement.amount!! * (source.amountLoc.divide(source.amountCurr))))

        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, source.id, AresConstants.UPDATE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)

        try {
            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = document?.organizationId))
        } catch (e: Exception) {
            Sentry.captureException(e)
        }
    }
    private suspend fun settleTaggedDocument(destinationDocument: AutoKnockoffDocumentResponse, sourceDocument: AutoKnockoffDocumentResponse, createdBy: UUID?): List<CheckDocument>? {
        val listOfDocuments = mutableListOf<AutoKnockoffDocumentResponse>()
        listOfDocuments.add(sourceDocument)
        listOfDocuments.add(destinationDocument)
        var settled: List<CheckDocument>? = null
        if (listOfDocuments.isNotEmpty()) {
            val documentEntity = listOfDocuments.map {
                val doc = it.accountUtilization!!
                Document(
                    id = doc.id!!,
                    tds = doc.tdsAmount!!,
                    documentNo = doc.documentNo,
                    documentValue = doc.documentValue!!,
                    accountType = doc.accType.name,
                    documentAmount = doc.amountCurr,
                    organizationId = doc.organizationId!!,
                    documentType = doc.accType.name,
                    mappingId = doc.tradePartyMappingId,
                    dueDate = doc.dueDate,
                    taxableAmount = doc.taxableAmount!!,
                    settledAmount = doc.payCurr,
                    balanceAmount = doc.amountCurr - doc.tdsAmount!! - doc.payCurr,
                    currency = doc.currency,
                    ledCurrency = doc.ledCurrency,
                    exchangeRate = it.exchangeRate,
                    signFlag = doc.signFlag,
                    approved = false,
                    accMode = doc.accMode,
                    documentDate = doc.transactionDate!!,
                    documentLedAmount = doc.amountLoc,
                    documentLedBalance = doc.amountLoc - doc.tdsAmountLoc!! - doc.payLoc,
                    sourceId = doc.documentNo,
                    sourceType = SettlementType.valueOf(doc.accType.name),
                    settledTds = BigDecimal.ZERO,
                    tdsCurrency = doc.currency,
                    afterTdsAmount = doc.amountCurr,
                    migrated = doc.migrated
                )
            }

            val documentModel = calculatingTds(documentEntity, sourceDocument.amount)

            val checkDocumentData = documentModel.map {
                CheckDocument(
                    id = it.id,
                    documentNo = it.documentNo,
                    tds = BigDecimal.ZERO,
                    afterTdsAmount = it.documentAmount,
                    documentValue = it.documentValue,
                    accountType = SettlementType.valueOf(it.accountType),
                    documentAmount = it.documentAmount,
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
                    signFlag = it.signFlag,
                    nostroAmount = it.nostroAmount,
                    settledTds = 0.toBigDecimal(),
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
    private suspend fun calculatingTds(documentEntity: List<Document?>, settlingAmount: BigDecimal): List<com.cogoport.ares.model.settlement.Document> {
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
