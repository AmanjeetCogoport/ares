package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.OrgIdAndEntityCode
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.entity.PaymentInvoiceMapping
import com.cogoport.ares.api.payment.mapper.PayableFileToPaymentMapper
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.InvoicePayMappingRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.ParentJVRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.ParentJVService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.common.KnockOffStatus
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountPayablesFile
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.PaymentInvoiceMappingType
import com.cogoport.ares.model.payment.RestoreUtrResponse
import com.cogoport.ares.model.payment.ReverseUtrRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.AccountPayableFileResponse
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import com.cogoport.ares.model.settlement.event.UpdateSettlementWhenBillUpdatedEvent
import io.micronaut.rabbitmq.exception.RabbitClientException
import io.sentry.Sentry
import jakarta.inject.Inject
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional
import kotlin.collections.HashMap

open class KnockoffServiceImpl : KnockoffService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject
    lateinit var payableFileToPaymentMapper: PayableFileToPaymentMapper

    @Inject
    lateinit var kuberMessagePublisher: KuberMessagePublisher

    @Inject
    lateinit var invoicePayMappingRepo: InvoicePayMappingRepository

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var parentJVRepository: ParentJVRepository

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    lateinit var parentJVService: ParentJVService

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class], dontRollbackOn = [RabbitClientException::class])
    override suspend fun uploadBillPayment(knockOffRecord: AccountPayablesFile): AccountPayableFileResponse {

        /* CHECK INVOICE/BILL EXISTS IN ACCOUNT UTILIZATION FOR THAT KNOCK OFF DOCUMENT*/
        val accountUtilization = accountUtilizationRepository.findRecord(knockOffRecord.documentNo, knockOffRecord.accType.name, AccMode.AP.name)
//        val isTransRefNumberExists = paymentRepository.isTransRefNumberExists(knockOffRecord.organizationId, knockOffRecord.transRefNumber)
//        if (accountUtilization == null || isTransRefNumberExists) {
        if (accountUtilization == null) {
            val accPayResponse = AccountPayableFileResponse(
                knockOffRecord.documentNo, knockOffRecord.documentValue, false,
                KnockOffStatus.UNPAID.name, Messages.NO_DOCUMENT_EXISTS,
                knockOffRecord.createdBy,
                null
            )
            emitPaymentStatus(accPayResponse)
            return accPayResponse
        }

        /*CREATE A NEW RECORD FOR THE PAYMENT TO VENDOR*/
        val paymentEntity = payableFileToPaymentMapper.convertToEntity(knockOffRecord)
        paymentEntity.paymentDocumentStatus = PaymentDocumentStatus.APPROVED
        val savedPaymentRecord = savePayment(
            paymentEntity, knockOffRecord.createdBy.toString(), knockOffRecord.performedByType
        )
        val previousSettlements = if (accountUtilization.accType in listOf(AccountType.PINV, AccountType.PREIMB)) {
            settlementRepository.getPaymentsCorrespondingDocumentNos(destinationId = knockOffRecord.documentNo, sourceId = null)
        } else {
            mutableListOf()
        }
        val tdsAmountPaid = previousSettlements.filter { it?.sourceType == SettlementType.VTDS }.sumOf { it?.amount ?: BigDecimal.ZERO }
        if (tdsAmountPaid != BigDecimal.ZERO) {
            val ledgerTotal = (accountUtilization.amountLoc.setScale(6, RoundingMode.UP))
            val grandTotal = (accountUtilization.amountCurr.setScale(6, RoundingMode.UP))
            val ledgerExchangeRate = (ledgerTotal.divide(grandTotal, 6))
            accountUtilization.payCurr -= tdsAmountPaid
            accountUtilization.payLoc -= (tdsAmountPaid * ledgerExchangeRate)
        }
        /*UPDATE THE AMOUNT PAID IN THE EXISTING BILL IN ACCOUNT UTILIZATION*/
        val currTotalAmtPaid = knockOffRecord.currencyAmount
        val ledTotalAmtPaid = knockOffRecord.ledgerAmount
        var amount = BigDecimal(0)
        var ledgerAmount = BigDecimal(0)
        var payCurrTds = BigDecimal.ZERO
        var payLocTds = BigDecimal.ZERO

        val isOverPaid = isOverPaid(accountUtilization, knockOffRecord.currencyAmount, knockOffRecord.ledgerAmount)

        /*IF TDS AMOUNT IS PRESENT  SAVE THE TDS SIMILARLY IN PAYMENT AND PAYMENT DISTRIBUTION*/
        if (knockOffRecord.currTdsAmount > BigDecimal.ZERO && knockOffRecord.ledTdsAmount > BigDecimal.ZERO &&
            (accountUtilization.isProforma == false) &&
            (accountUtilization.createdAt!! >= Timestamp.from(LocalDate.of(2023, 7, 28).atStartOfDay().toInstant(ZoneOffset.UTC)))
        ) {
            paymentEntity.amount = knockOffRecord.currTdsAmount
            paymentEntity.ledAmount = knockOffRecord.ledTdsAmount

            if ((accountUtilization.tdsAmount?.minus(tdsAmountPaid)!! < knockOffRecord.currTdsAmount) && (accountUtilization.tdsAmountLoc?.minus(tdsAmountPaid * (accountUtilization.amountLoc.divide(accountUtilization.amountCurr)))!! < knockOffRecord.ledTdsAmount)) {
                payCurrTds = accountUtilization.tdsAmount!! - tdsAmountPaid
                payLocTds = accountUtilization.tdsAmountLoc!! - tdsAmountPaid * (accountUtilization.amountLoc.divide(accountUtilization.amountCurr))
            } else {
                payCurrTds = knockOffRecord.currTdsAmount
                payLocTds = knockOffRecord.ledTdsAmount
            }

            // creating jv for tds on invoice
            val jvIdAsSourceId = saveTdsAsJv(
                knockOffRecord.currency,
                knockOffRecord.ledgerCurrency,
                tdsAmount = knockOffRecord.currTdsAmount,
                tdsLedAmount = knockOffRecord.ledTdsAmount,
                createdBy = knockOffRecord.createdBy,
                knockOffRecord.performedByType,
                accountUtilization,
                exchangeRate = knockOffRecord.ledgerAmount.divide(knockOffRecord.currencyAmount)
                    .setScale(AresConstants.DECIMAL_NUMBER_UPTO, RoundingMode.HALF_DOWN),
                paymentTransactionDate = knockOffRecord.transactionDate,
                utr = knockOffRecord.transRefNumber,
                payCurrTds,
                payLocTds
            )

            saveSettlements(
                knockOffRecord,
                destinationId = knockOffRecord.documentNo,
                sourceId = jvIdAsSourceId,
                true,
                amount = payCurrTds,
                ledAmount = payLocTds
            )
            saveInvoicePaymentMapping(jvIdAsSourceId!!, knockOffRecord, isTDSEntry = true, knockOffRecord.documentNo, payCurrTds, payLocTds)
        }

        if (isOverPaid) {
            amount = accountUtilization.amountCurr - accountUtilization.tdsAmount!! - accountUtilization.payCurr
            ledgerAmount = accountUtilization.amountLoc - accountUtilization.tdsAmountLoc!! - accountUtilization.payLoc
        } else {
            amount = currTotalAmtPaid
            ledgerAmount = ledTotalAmtPaid
        }

        accountUtilizationRepository.updateInvoicePayment(accountUtilization.id!!, amount + payCurrTds, ledgerAmount + payLocTds)

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accountUtilization.id,
                actionName = AresConstants.UPDATE,
                data = mapOf("pay_curr" to currTotalAmtPaid, "pay_loc" to ledTotalAmtPaid),
                performedBy = knockOffRecord.updatedBy.toString(),
                performedByUserType = knockOffRecord.performedByType
            )
        )

        var settlementNum: String? = null

        if (amount > BigDecimal.ZERO && ledgerAmount > BigDecimal.ZERO) {
            settlementNum = saveSettlements(knockOffRecord, accountUtilization.documentNo, savedPaymentRecord.paymentNum, false, amount, ledgerAmount)
        }

        saveAccountUtilization(
            savedPaymentRecord.paymentNum!!, savedPaymentRecord.paymentNumValue!!, knockOffRecord, accountUtilization,
            currTotalAmtPaid, ledTotalAmtPaid, amount,
            ledgerAmount
        )

        /*SAVE THE PAYMENT DISTRIBUTION AGAINST THE INVOICE */
        saveInvoicePaymentMapping(savedPaymentRecord.id!!, knockOffRecord, false, knockOffRecord.documentNo, amount, ledgerAmount) // TODO(LED AMOUNT)

        var paymentStatus = KnockOffStatus.PARTIAL.name
        val leftAmount = (accountUtilization.amountLoc - accountUtilization.tdsAmountLoc!!) - (accountUtilization.payLoc + ledTotalAmtPaid)

        if (leftAmount <= 1.toBigDecimal() || leftAmount.setScale(2, RoundingMode.HALF_UP) <= 1.toBigDecimal())
            paymentStatus = KnockOffStatus.FULL.name

        val accPayResponse = AccountPayableFileResponse(knockOffRecord.documentNo, knockOffRecord.documentValue, true, paymentStatus, null, knockOffRecord.createdBy, settlementNum)
        try {
            emitPaymentStatus(accPayResponse)
            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = knockOffRecord.organizationId))
        } catch (k: RabbitClientException) {
            logger().error(k.stackTraceToString())
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
            Sentry.captureException(e)
        }
        return accPayResponse
    }

    private fun isOverPaid(accountUtilization: AccountUtilization, currTotalAmtPaid: BigDecimal, ledTotalAmtPaid: BigDecimal): Boolean {
        if ((accountUtilization.amountCurr - accountUtilization.tdsAmount!!) < accountUtilization.payCurr + currTotalAmtPaid && (accountUtilization.amountLoc - accountUtilization.tdsAmountLoc!!) < accountUtilization.payLoc + ledTotalAmtPaid)
            return true
        return false
    }
    /**
     * Emits Kafka message on topic <b>payables-bill-status</b>
     * @param : accPayResponseList
     */
    private suspend fun emitPaymentStatus(accPayResponseList: AccountPayableFileResponse) {
        var event = com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent(accPayResponseList)
        kuberMessagePublisher.emitBillPaymentStatus(event)
    }

    private suspend fun savePayment(paymentEntity: Payment, performedBy: String? = null, performedByType: String? = null): Payment {
        paymentEntity.paymentCode = PaymentCode.PAY
        paymentEntity.accCode = AresModelConstants.AP_ACCOUNT_CODE
        paymentEntity.createdAt = Timestamp.from(Instant.now())
        paymentEntity.updatedAt = Timestamp.from(Instant.now())
        paymentEntity.signFlag = SignSuffix.PAY.sign

        /*GENERATING A UNIQUE RECEIPT NUMBER FOR PAYMENT*/
        val financialYearSuffix = sequenceGeneratorImpl.getFinancialYearSuffix()
        paymentEntity.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.PAYMENT.prefix)
        paymentEntity.paymentNumValue = SequenceSuffix.PAYMENT.prefix + financialYearSuffix + paymentEntity.paymentNum

        paymentEntity.migrated = false
        /* CREATE A NEW RECORD FOR THE PAYMENT AND SAVE THE PAYMENT IN DATABASE*/
        paymentEntity.paymentDocumentStatus = paymentEntity.paymentDocumentStatus ?: PaymentDocumentStatus.CREATED
        val paymentObj = paymentRepository.save(paymentEntity)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PAYMENTS,
                objectId = paymentObj.id,
                actionName = AresConstants.CREATE,
                data = paymentObj,
                performedBy = performedBy,
                performedByUserType = performedByType
            )
        )
        return paymentObj
    }

    private suspend fun saveInvoicePaymentMapping(paymentId: Long, knockOffRecord: AccountPayablesFile, isTDSEntry: Boolean, documentNo: Long, payCurrTds: BigDecimal?, payLocTds: BigDecimal?) {
        val invoicePayMap = PaymentInvoiceMapping(
            id = null,
            accountMode = AccMode.AP,
            documentNo = documentNo,
            paymentId = paymentId,
            mappingType = if (!isTDSEntry) PaymentInvoiceMappingType.BILL.name else PaymentInvoiceMappingType.TDS.name,
            currency = knockOffRecord.currency,
            ledCurrency = knockOffRecord.ledgerCurrency,
            signFlag = SignSuffix.PAY.sign,
            amount = payCurrTds!!,
            ledAmount = payLocTds!!,
            transactionDate = knockOffRecord.transactionDate,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now())
        )
        invoicePayMappingRepo.save(invoicePayMap)
    }

    private suspend fun saveAccountUtilization(
        paymentNum: Long,
        paymentNumValue: String,
        knockOffRecord: AccountPayablesFile,
        accountUtilization: AccountUtilization,
        currTotalAmtPaid: BigDecimal,
        ledTotalAmtPaid: BigDecimal,
        utilizedCurrTotalAmtPaid: BigDecimal,
        utilizedLedTotalAmtPaid: BigDecimal
    ) {
        val accountUtilEntity = AccountUtilization(
            id = null,
            documentNo = paymentNum,
            documentValue = paymentNumValue,
            zoneCode = knockOffRecord.zoneCode.toString(),
            serviceType = accountUtilization.serviceType,
            documentStatus = DocumentStatus.FINAL,
            entityCode = knockOffRecord.entityCode,
            category = knockOffRecord.category,
            sageOrganizationId = null,
            organizationId = knockOffRecord.organizationId!!,
            taggedOrganizationId = knockOffRecord.taggedOrganizationId,
            tradePartyMappingId = knockOffRecord.tradePartyMappingId,
            organizationName = knockOffRecord.organizationName,
            accCode = AresModelConstants.AP_ACCOUNT_CODE,
            accType = AccountType.PAY,
            accMode = knockOffRecord.accMode,
            signFlag = SignSuffix.PAY.sign,
            currency = knockOffRecord.currency,
            ledCurrency = knockOffRecord.ledgerCurrency,
            amountCurr = currTotalAmtPaid,
            amountLoc = ledTotalAmtPaid,
            payCurr = utilizedCurrTotalAmtPaid,
            payLoc = utilizedLedTotalAmtPaid,
            taxableAmount = BigDecimal.ZERO,
            tdsAmountLoc = BigDecimal.ZERO,
            tdsAmount = BigDecimal.ZERO,
            dueDate = accountUtilization.dueDate,
            transactionDate = knockOffRecord.transactionDate,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now()),
            orgSerialId = knockOffRecord.orgSerialId,
            migrated = false,
            settlementEnabled = true,
            isProforma = false
        )
        val accUtilObj = accountUtilizationRepository.save(accountUtilEntity)

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accUtilObj.id,
                actionName = AresConstants.CREATE,
                data = accUtilObj,
                performedBy = knockOffRecord.updatedBy.toString(),
                performedByUserType = knockOffRecord.performedByType
            )
        )

        try {
            if (accUtilObj.accMode == AccMode.AR) {
                aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountUtilEntity.organizationId))
                aresMessagePublisher.emitUpdateCustomerDetail(OrgIdAndEntityCode(accountUtilEntity.organizationId!!, accountUtilEntity.entityCode))
            }
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
            Sentry.captureException(e)
        }
    }

    private suspend fun saveSettlements(
        knockOffRecord: AccountPayablesFile,
        destinationId: Long?,
        sourceId: Long?,
        isTDSEntry: Boolean,
        amount: BigDecimal,
        ledAmount: BigDecimal
    ): String? {
        val settlement = generateSettlementEntity(knockOffRecord, destinationId, sourceId, isTDSEntry, amount, ledAmount)
        settlement.settlementNum = sequenceGeneratorImpl.getSettlementNumber()
        val settleObj = settlementRepository.save(settlement)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.SETTLEMENT,
                objectId = settleObj.id,
                actionName = AresConstants.CREATE,
                data = settleObj,
                performedBy = knockOffRecord.createdBy.toString(),
                performedByUserType = knockOffRecord.performedByType
            )
        )
        return settleObj.settlementNum
    }

    private fun generateSettlementEntity(
        knockOffRecord: AccountPayablesFile,
        destinationId: Long?,
        sourceId: Long?,
        isTDSEntry: Boolean,
        amount: BigDecimal,
        ledAmount: BigDecimal
    ): Settlement {
        return Settlement(
            id = null,
            sourceId = sourceId,
            sourceType = if (isTDSEntry) SettlementType.VTDS else SettlementType.PAY,
            destinationId = destinationId!!,
            destinationType = SettlementType.PINV,
            ledCurrency = knockOffRecord.ledgerCurrency,
            ledAmount = ledAmount,
            currency = knockOffRecord.currency,
            amount = amount,
            signFlag = if (isTDSEntry) -1 else 1,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now()),
            createdBy = knockOffRecord.createdBy,
            updatedBy = knockOffRecord.updatedBy,
            settlementDate = Date(Timestamp.from(Instant.now()).time),
            settlementNum = null,
            settlementStatus = SettlementStatus.CREATED
        )
    }
    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun reverseUtr(reverseUtrRequest: ReverseUtrRequest) {
        val accountUtilization = accountUtilizationRepository.findRecord(documentNo = reverseUtrRequest.documentNo, accMode = AccMode.AP.name)
        val payments = paymentRepository.findByTransRef(reverseUtrRequest.transactionRef)
        val tdsJvRecord = journalVoucherRepository.findByDescription(reverseUtrRequest.transactionRef)
        var tdsPaid = 0.toBigDecimal()
        var ledTdsPaid = 0.toBigDecimal()

        if (payments.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1546, "")
        }

        val sourceIdsForSettlement = listOfNotNull(payments[0].paymentNum, tdsJvRecord?.id)
        val settlementData = settlementRepository.getSettlementByDestinationId(reverseUtrRequest.documentNo, sourceIdsForSettlement)

        if (settlementData.any { it.settlementStatus == SettlementStatus.POSTED }) {
            throw AresException(AresError.ERR_1544, "")
        }

        val settlementIds = settlementData.map { it.id }

        for (payment in payments) {
            val paymentInvoiceMappingData = invoicePayMappingRepo.findByPaymentId(reverseUtrRequest.documentNo, payment.id)
            paymentRepository.deletePayment(payment.id)
            invoicePayMappingRepo.deletePaymentMappings(paymentInvoiceMappingData.id)
            createAudit(AresConstants.PAYMENTS, payment.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            createAudit("payment_invoice_map", paymentInvoiceMappingData.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        }

        if (tdsJvRecord != null) {
            val tdsJvMappingData = invoicePayMappingRepo.findByPaymentId(reverseUtrRequest.documentNo, tdsJvRecord.id)
            val tdsJvAccountUtilRecord = accountUtilizationRepository.findRecord(tdsJvRecord.id!!, tdsJvRecord.category, tdsJvRecord.accMode?.name)
            tdsPaid = tdsJvAccountUtilRecord?.payCurr!!
            ledTdsPaid = tdsJvAccountUtilRecord.payLoc
            invoicePayMappingRepo.deletePaymentMappings(tdsJvMappingData.id)
            createAudit("payment_invoice_map", tdsJvMappingData.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            parentJVRepository.deleteJournalVoucherById(tdsJvRecord.parentJvId!!, reverseUtrRequest.updatedBy!!)
            createAudit("parent_journal_vouchers", tdsJvRecord.parentJvId, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            journalVoucherRepository.deleteJvLineItemByParentJvId(tdsJvRecord.parentJvId!!, reverseUtrRequest.updatedBy!!)
            createAudit("journal_vouchers", tdsJvRecord.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            accountUtilizationRepository.deleteAccountUtilization(tdsJvAccountUtilRecord.id!!)
            createAudit("account_utillizations", tdsJvAccountUtilRecord.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        }

        settlementRepository.deleleSettlement(settlementIds)
        createAudit(AresConstants.SETTLEMENT, settlementIds[0], AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        if (settlementIds.size > 1) {
            createAudit(AresConstants.SETTLEMENT, settlementIds[1], AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        }

        val accountUtilizationPaymentData = accountUtilizationRepository.getDataByPaymentNum(payments[0].paymentNum)
        accountUtilizationRepository.deleteAccountUtilization(accountUtilizationPaymentData.id)
        var leftAmountPayCurr: BigDecimal? = accountUtilization?.payCurr?.minus(accountUtilizationPaymentData.payCurr)?.minus(tdsPaid)
        var leftAmountLedgerCurr: BigDecimal? = accountUtilization?.payLoc?.minus(accountUtilizationPaymentData.payLoc)?.minus(ledTdsPaid)

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
                leftAmountPayCurr.compareTo(accountUtilization?.amountCurr) == 0 -> {
                    KnockOffStatus.FULL
                }
                else -> {
                    KnockOffStatus.PARTIAL
                }
            }
        }
        accountUtilizationRepository.updateAccountUtilization(accountUtilization?.id!!, accountUtilization.documentStatus!!, leftAmountPayCurr!!, leftAmountLedgerCurr!!)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilizationPaymentData.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilization.id!!, AresConstants.UPDATE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)

        aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountUtilization.organizationId))
        aresMessagePublisher.emitUpdateCustomerDetail(OrgIdAndEntityCode(accountUtilization.organizationId!!, accountUtilization.entityCode))

        kuberMessagePublisher.emitPostRestoreUtr(
            restoreUtrResponse = RestoreUtrResponse(
                documentNo = reverseUtrRequest.documentNo,
                paidAmount = accountUtilizationPaymentData.payCurr,
                paidTds = BigDecimal.ZERO,
                paymentStatus = paymentStatus,
                paymentUploadAuditId = reverseUtrRequest.paymentUploadAuditId,
                updatedBy = reverseUtrRequest.updatedBy,
                performedByType = reverseUtrRequest.performedByType
            )
        )
        try {
            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accountUtilization.organizationId))
        } catch (e: Exception) {
            Sentry.captureException(e)
        }
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

    override suspend fun editSettlementWhenBillUpdated(updateRequest: UpdateSettlementWhenBillUpdatedEvent) {
        var newBillAmount = updateRequest.updateBillAmount
        var newLedgerAmount = updateRequest.updateLedgerAmount
        val settlementDetails = settlementRepository.findByDestIdAndDestTypeAndSourceType(updateRequest.billId, SettlementType.PINV, SettlementType.PAY)

        var paymentData = hashMapOf<Long, BigDecimal?>()
        var indexToStop = settlementDetails.size - 1
        var stopSettlementIteration = false

        for ((index, settlement) in settlementDetails.withIndex()) {
            if (stopSettlementIteration) {
                break
            }
            var newSettlement = when {
                settlement?.amount!! < newBillAmount -> {
                    settlement
                }
                settlement.amount!! > newBillAmount -> {
                    if (paymentData.containsKey(settlement.sourceId)) {
                        paymentData[settlement.sourceId!!] = paymentData[settlement.sourceId]?.plus(settlement.amount!! - newBillAmount)
                    } else {
                        paymentData[settlement.sourceId!!] = settlement.amount!! - newBillAmount
                    }
                    indexToStop = index + 1
                    stopSettlementIteration = true
                    settledAmountGreaterThanNewBillAmount(settlement, newBillAmount, newLedgerAmount)
                }
                else -> {
                    indexToStop = index + 1
                    stopSettlementIteration = true
                    settlement
                }
            }
            newBillAmount = newBillAmount.minus(newSettlement.amount!!)
            newLedgerAmount = newLedgerAmount.minus(newSettlement.ledAmount)
        }

        val settlementIdForDelete = mutableListOf<Long>()

        for (index in indexToStop.until(settlementDetails.size)) {
            settlementIdForDelete.add(settlementDetails[index]?.id!!)
            val key = settlementDetails[index]?.sourceId
            val amount = settlementDetails[index]?.amount
            if (paymentData.containsKey(key)) {
                paymentData[key!!] = paymentData[key]?.plus(amount!!)
            } else {
                paymentData[key!!] = amount!!
            }
        }

        settlementRepository.deleleSettlement(settlementIdForDelete.toList())

        paymentData.keys.forEach { paymentNum ->
            val paymentDetails = accountUtilizationRepository.findRecord(paymentNum, AccountType.PAY.name, AccMode.AP.name)
            paymentDetails?.payCurr = paymentDetails?.payCurr?.minus(paymentData[paymentNum]!!)!!
            paymentDetails.payLoc = paymentDetails.payLoc.minus(paymentData[paymentNum]!!)
            accountUtilizationRepository.update(paymentDetails)
        }

        val newSettlementDetails = settlementRepository.findByDestIdAndDestTypeAndSourceType(updateRequest.billId, SettlementType.PINV, SettlementType.PAY)
        var newPayCurr: BigDecimal = 0.toBigDecimal()
        var newPayLoc: BigDecimal = 0.toBigDecimal()
        for (settlement in newSettlementDetails) {
            newPayCurr += settlement?.amount!!
            newPayLoc += settlement.ledAmount
        }
        accountUtilizationRepo.updateAccountUtilizationByDocumentNo(updateRequest.billId, newPayCurr, newPayLoc, AccountType.PINV)
    }

    private suspend fun settledAmountGreaterThanNewBillAmount(settlement: Settlement, billAmount: BigDecimal, ledgerAmount: BigDecimal): Settlement {
        settlement.amount = billAmount
        settlement.ledAmount = ledgerAmount
        settlementRepository.deleleSettlement(arrayListOf(settlement.id!!))
        return settlementRepository.save(settlement)
    }

    private suspend fun saveTdsAsJv(
        currency: String?,
        ledCurrency: String,
        tdsAmount: BigDecimal,
        tdsLedAmount: BigDecimal,
        createdBy: UUID?,
        createdByUserType: String?,
        accountUtilization: AccountUtilization?,
        exchangeRate: BigDecimal?,
        paymentTransactionDate: Date,
        utr: String?,
        payCurrTds: BigDecimal?,
        payLocTds: BigDecimal?
    ): Long? {
        val lineItemProps: MutableList<HashMap<String, Any?>> = mutableListOf(
            hashMapOf(
                "accMode" to "AP",
                "glCode" to "321000",
                "type" to "DEBIT",
                "signFlag" to 1
            ),
            hashMapOf(
                "accMode" to "VTDS",
                "glCode" to "324003",
                "type" to "CREDIT",
                "signFlag" to -1
            )
        )
        return parentJVService.createTdsAsJvForBills(
            currency,
            ledCurrency,
            tdsAmount,
            tdsLedAmount,
            createdBy,
            createdByUserType,
            accountUtilization,
            exchangeRate,
            paymentTransactionDate,
            lineItemProps,
            utr,
            payCurrTds,
            payLocTds
        )
    }
}
