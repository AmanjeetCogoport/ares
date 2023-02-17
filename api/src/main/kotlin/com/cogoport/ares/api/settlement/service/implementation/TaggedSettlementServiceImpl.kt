package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.AutoKnockoffDocumentResponse
import com.cogoport.ares.api.payment.model.ReversePaymentRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.InvoicePayMappingRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.TaggedSettlementService
import com.cogoport.ares.model.common.KnockOffStatus
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PaymentInvoiceMappingType
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Inject
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.util.UUID
import javax.transaction.Transactional

class TaggedSettlementServiceImpl : TaggedSettlementService {

    @Inject
    private lateinit var settlementRepository: SettlementRepository

    @Inject
    private lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    private lateinit var paymentRepository: PaymentRepository

    @Inject
    private lateinit var invoicePayMappingRepo: InvoicePayMappingRepository

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

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun settleOnAccountInvoicePayment(req: OnAccountPaymentRequest) {
        val destinationDocument = AutoKnockoffDocumentResponse()
        val extendedSourceDocument = mutableListOf<AutoKnockoffDocumentResponse?>()
        req.document.documentNo = Hashids.decode(req.document.documentNo)[0].toString()
        val taggedDocumentIds = req.taggedDocuments.map { Hashids.decode(it.documentNo)[0] }
        val settledSourceDocuments = settlementRepository.getPaymentsCorrespondingDocumentNo(taggedDocumentIds)
        val sourceType = settledSourceDocuments[0]?.sourceType!!.name
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
                    payableTds = documentInfo?.payableTds
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
        var balanceSettlingAmount = destinationDocument.accountUtilization?.amountCurr!! - destinationDocument.accountUtilization?.payCurr!!
        var sourceStartIndex = 0
        val sourceEndIndex = extendedSourceDocument.size
        while (balanceSettlingAmount.compareTo(BigDecimal.ZERO) > 0 && sourceStartIndex < sourceEndIndex) {
            var settledList: List<CheckDocument>?
            destinationDocument.accountUtilization = accountUtilizationRepository.findRecord(
                destinationDocument.accountUtilization?.documentNo!!,
                AccountType.PINV.name,
                null
            )
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
                reversePayment(
                    ReversePaymentRequest(
                        payment?.destinationId!!.toLong(),
                        payment.transRefNumber!!,
                        req.createdBy,
                        null
                    )
                )
                source.accountUtilization = accountUtilizationRepository.findRecord(
                    source.accountUtilization?.documentNo!!,
                    source.accountUtilization?.accType!!.name,
                    null
                )
                settledList = settleTaggedDocument(destinationDocument, source, req.createdBy)
            }
            balanceSettlingAmount = settledList?.get(1)?.balanceAmount!!
            sourceStartIndex++
        }
    }
    private suspend fun reversePayment(reversePaymentRequest: ReversePaymentRequest) {
        val accountUtilization = accountUtilizationRepository.findRecord(reversePaymentRequest.document, AccountType.PINV.name, AccMode.AP.name)
        val payments = paymentRepository.findByTransRef(reversePaymentRequest.transferReferNo)
        var tdsPaid = 0.toBigDecimal()
        var amountPaid: BigDecimal = 0.toBigDecimal()

        for (payment in payments) {
            val paymentInvoiceMappingData = invoicePayMappingRepo.findByPaymentId(accountUtilization!!.documentNo, payment.id)
            if (paymentInvoiceMappingData.mappingType == PaymentInvoiceMappingType.BILL) {
                amountPaid = paymentInvoiceMappingData.amount
            } else if (paymentInvoiceMappingData.mappingType == PaymentInvoiceMappingType.TDS) {
                tdsPaid = paymentInvoiceMappingData.amount
            }
            createAudit(AresConstants.PAYMENTS, payment.id, AresConstants.DELETE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
            createAudit("payment_invoice_map", paymentInvoiceMappingData.id, AresConstants.DELETE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        }

        val settlementIds = settlementRepository.getSettlementByDestinationId(accountUtilization!!.documentNo, payments[0].paymentNum!!)
        settlementRepository.deleleSettlement(settlementIds) // TODO("Don't Delete The Settlement, Mark it as Draft, LEFT all the table same")

        createAudit(AresConstants.SETTLEMENT, settlementIds[0], AresConstants.DELETE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.SETTLEMENT, settlementIds[1], AresConstants.DELETE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        val accountUtilizationPaymentData = accountUtilizationRepository.getDataByPaymentNum(payments[0].paymentNum)
        accountUtilizationRepository.markAccountUtilizationDraft(accountUtilizationPaymentData.id, BigDecimal.ZERO, BigDecimal.ZERO) // TODO("nothing")
        var leftAmountPayCurr: BigDecimal? = accountUtilization.payCurr.minus(accountUtilizationPaymentData.payCurr)
        var leftAmountLedgerCurr: BigDecimal? = accountUtilization.payLoc.minus(accountUtilizationPaymentData.payLoc)

        leftAmountPayCurr = if (leftAmountPayCurr?.setScale(2, RoundingMode.HALF_UP) == 0.toBigDecimal()) {
            0.toBigDecimal()
        } else {
            leftAmountPayCurr
        }
        leftAmountLedgerCurr = if (leftAmountLedgerCurr?.setScale(2, RoundingMode.HALF_UP) == 0.toBigDecimal()) {
            0.toBigDecimal()
        } else {
            leftAmountLedgerCurr
        }

        var paymentStatus: KnockOffStatus = KnockOffStatus.UNPAID
        if (leftAmountPayCurr != null) {
            paymentStatus = when {
                leftAmountPayCurr.compareTo(BigDecimal.ZERO) == 0 -> {
                    KnockOffStatus.UNPAID
                }
                leftAmountPayCurr.compareTo(accountUtilization.amountCurr) == 0 -> {
                    KnockOffStatus.FULL
                }
                else -> {
                    KnockOffStatus.PARTIAL
                }
            }
        }
        accountUtilizationRepository.updateAccountUtilization(
            accountUtilization.id!!,
            DocumentStatus.DRAFT, leftAmountPayCurr!!, leftAmountLedgerCurr!!
        )
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilizationPaymentData.id, AresConstants.DELETE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilization.id!!, AresConstants.UPDATE, null, reversePaymentRequest.updatedBy.toString(), reversePaymentRequest.performedByType)

//        try {
//        kuberMessagePublisher.emitRestorePayment(
//            restoreUtrResponse = RestoreUtrResponse(
//                documentNo = reversePaymentRequest.document,
//                paidAmount = amountPaid,
//                paidTds = tdsPaid,
//                paymentStatus = paymentStatus,
//                paymentUploadAuditId = 123,
//                updatedBy = reversePaymentRequest.updatedBy,
//                performedByType = reversePaymentRequest.performedByType
//            )
//        )
//            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accountUtilization.organizationId))
//        } catch (e: Exception) {
//            Sentry.captureException(e)
//        }
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
                    exchangeRate = 1.toBigDecimal(),
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
