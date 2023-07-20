package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
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
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.ParentJVRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
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
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import com.cogoport.ares.model.settlement.event.UpdateSettlementWhenBillUpdatedEvent
import io.micronaut.rabbitmq.exception.RabbitClientException
import io.sentry.Sentry
import jakarta.inject.Inject
import kotlinx.coroutines.flow.toList
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
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

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class], dontRollbackOn = [RabbitClientException::class])
    override suspend fun uploadBillPayment(knockOffRecord: AccountPayablesFile): AccountPayableFileResponse {

        /* CHECK INVOICE/BILL EXISTS IN ACCOUNT UTILIZATION FOR THAT KNOCK OFF DOCUMENT*/
        val accountUtilization = accountUtilizationRepository.findRecord(knockOffRecord.documentNo, knockOffRecord.accType.name, AccMode.AP.name)
        val isTransRefNumberExists = paymentRepository.isTransRefNumberExists(knockOffRecord.organizationId, knockOffRecord.transRefNumber)
        if (accountUtilization == null || isTransRefNumberExists) {
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
            paymentEntity, isTDSEntry = false, knockOffRecord.createdBy.toString(), knockOffRecord.performedByType
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

        val isOverPaid = isOverPaid(accountUtilization, knockOffRecord.currencyAmount, knockOffRecord.ledgerAmount)

        if (isOverPaid) {
            accountUtilizationRepository.updateInvoicePayment(accountUtilization.id!!, (accountUtilization.amountCurr - accountUtilization.tdsAmount!! - accountUtilization.payCurr), (accountUtilization.amountLoc - accountUtilization.tdsAmountLoc!! - accountUtilization.payLoc))
        } else {
            accountUtilizationRepository.updateInvoicePayment(accountUtilization.id!!, currTotalAmtPaid, ledTotalAmtPaid)
        }
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

        val settlementNum = saveSettlements(knockOffRecord, accountUtilization.documentNo, savedPaymentRecord.paymentNum, isOverPaid, accountUtilization, false)
        val settlementNum2 = saveSettlements(knockOffRecord, accountUtilization.documentNo, savedPaymentRecord.paymentNum, isOverPaid, accountUtilization, true)
        /* SAVE THE ACCOUNT UTILIZATION FOR THE NEWLY PAYMENT DONE*/

        if (isOverPaid) {
            saveAccountUtilization(
                savedPaymentRecord.paymentNum!!, savedPaymentRecord.paymentNumValue!!, knockOffRecord, accountUtilization,
                currTotalAmtPaid, ledTotalAmtPaid, (accountUtilization.amountCurr - accountUtilization.tdsAmount!! - accountUtilization.payCurr),
                (accountUtilization.amountLoc - accountUtilization.tdsAmountLoc!! - accountUtilization.payLoc), AccountType.PAY
            )
        } else {
            saveAccountUtilization(
                savedPaymentRecord.paymentNum!!, savedPaymentRecord.paymentNumValue!!, knockOffRecord, accountUtilization,
                currTotalAmtPaid, ledTotalAmtPaid, currTotalAmtPaid, ledTotalAmtPaid, AccountType.PAY
            )
        }
        /*IF TDS AMOUNT IS PRESENT  SAVE THE TDS SIMILARLY IN PAYMENT AND PAYMENT DISTRIBUTION*/
        if (knockOffRecord.currTdsAmount > BigDecimal.ZERO && knockOffRecord.ledTdsAmount > BigDecimal.ZERO) {
            paymentEntity.amount = knockOffRecord.currTdsAmount
            paymentEntity.ledAmount = knockOffRecord.ledTdsAmount

            // creating jv for tds on invoice
            val jvRecord = saveTdsAsJv(knockOffRecord, accountUtilization)
            val savedTDSPaymentRecord = savePayment(
                paymentEntity, isTDSEntry = true, knockOffRecord.createdBy.toString(), knockOffRecord.performedByType
            )

            saveAccountUtilization(
                savedTDSPaymentRecord.paymentNum!!,
                savedTDSPaymentRecord.paymentNumValue!!,
                knockOffRecord,
                accountUtilization,
                currTotalAmtPaid = savedTDSPaymentRecord.amount,
                ledTotalAmtPaid = savedTDSPaymentRecord.ledAmount!!,
                utilizedCurrTotalAmtPaid = savedTDSPaymentRecord.amount,
                utilizedLedTotalAmtPaid = savedTDSPaymentRecord.ledAmount!!,
                accountType = AccountType.VTDS
            )

            saveSettlements(
                knockOffRecord,
                destinationId = jvRecord.id,
                sourceId = savedPaymentRecord.paymentNum,
                false,
                accountUtilization,
                false
            )
            saveInvoicePaymentMapping(savedTDSPaymentRecord.id!!, knockOffRecord, isTDSEntry = true, jvRecord.id!!)
        }

        /*SAVE THE PAYMENT DISTRIBUTION AGAINST THE INVOICE */
        saveInvoicePaymentMapping(savedPaymentRecord.id!!, knockOffRecord, false, knockOffRecord.documentNo) // TODO(LED AMOUNT)

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

    private suspend fun savePayment(paymentEntity: Payment, isTDSEntry: Boolean, performedBy: String? = null, performedByType: String? = null): Payment {
        paymentEntity.paymentCode = if (!isTDSEntry) PaymentCode.PAY else PaymentCode.VTDS
        paymentEntity.accCode = if (!isTDSEntry) AresModelConstants.AP_ACCOUNT_CODE else AresModelConstants.TDS_AP_ACCOUNT_CODE
        paymentEntity.createdAt = Timestamp.from(Instant.now())
        paymentEntity.updatedAt = Timestamp.from(Instant.now())
        paymentEntity.signFlag = SignSuffix.PAY.sign

        /*GENERATING A UNIQUE RECEIPT NUMBER FOR PAYMENT*/
        if (!isTDSEntry) {
            paymentEntity.paymentNum = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.PAYMENT.prefix)
            val financialYearSuffix = sequenceGeneratorImpl.getFinancialYearSuffix()
            paymentEntity.paymentNumValue = SequenceSuffix.PAYMENT.prefix + financialYearSuffix + paymentEntity.paymentNum
        }
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

    private suspend fun saveInvoicePaymentMapping(paymentId: Long, knockOffRecord: AccountPayablesFile, isTDSEntry: Boolean, documentNo: Long) {
        val invoicePayMap = PaymentInvoiceMapping(
            id = null,
            accountMode = AccMode.AP,
            documentNo = documentNo,
            paymentId = paymentId,
            mappingType = if (!isTDSEntry) PaymentInvoiceMappingType.BILL.name else PaymentInvoiceMappingType.TDS.name,
            currency = knockOffRecord.currency,
            ledCurrency = knockOffRecord.ledgerCurrency,
            signFlag = SignSuffix.PAY.sign,
            amount = if (!isTDSEntry) knockOffRecord.currencyAmount else knockOffRecord.currTdsAmount,
            ledAmount = if (!isTDSEntry) knockOffRecord.ledgerAmount else knockOffRecord.ledTdsAmount,
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
        utilizedLedTotalAmtPaid: BigDecimal,
        accountType: AccountType
    ) {
        val signFlag = when (accountType) {
            AccountType.VTDS, AccountType.PAY -> SignSuffix.PAY.sign
            else -> SignSuffix.JVTDS.sign
        }
        val accCode = when (accountType) {
            AccountType.VTDS, AccountType.PAY -> AresModelConstants.AP_ACCOUNT_CODE
            else -> AresModelConstants.TDS_AP_ACCOUNT_CODE
        }
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
            accCode = accCode,
            accType = accountType,
            accMode = knockOffRecord.accMode,
            signFlag = signFlag,
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
        isOverPaid: Boolean,
        accountUtilization: AccountUtilization,
        isTDSEntry: Boolean
    ): String? {

        val settlement = generateSettlementEntity(knockOffRecord, destinationId, sourceId, isTDSEntry)
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
        isTDSEntry: Boolean
    ): Settlement {
        return Settlement(
            id = null,
            sourceId = sourceId,
            sourceType = if (isTDSEntry) SettlementType.VTDS else SettlementType.PAY,
            destinationId = destinationId!!,
            destinationType = if (isTDSEntry) SettlementType.JVTDS else SettlementType.PINV,
            ledCurrency = knockOffRecord.ledgerCurrency,
            ledAmount = if (isTDSEntry) knockOffRecord.ledTdsAmount else knockOffRecord.ledgerAmount,
            currency = knockOffRecord.currency,
            amount = if (isTDSEntry) knockOffRecord.currTdsAmount else knockOffRecord.currencyAmount,
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
        val accountUtilization = accountUtilizationRepository.findRecordByDocumentValue(
            documentValue = reverseUtrRequest.documentValue,
            accMode = AccMode.AP.name
        )
        val payments = paymentRepository.findByTransRef(reverseUtrRequest.transactionRef)
        var tdsPaid = 0.toBigDecimal()
        var ledTdsPaid = 0.toBigDecimal()
        var amountPaid: BigDecimal = 0.toBigDecimal()
        var ledTotalAmtPaid: BigDecimal = 0.toBigDecimal()

        var paymentStatus: KnockOffStatus = KnockOffStatus.UNPAID

        for (payment in payments) {
            val paymentInvoiceMappingData = invoicePayMappingRepo.findByPaymentId(payment.id)
            val destinationType = if (paymentInvoiceMappingData.mappingType == PaymentInvoiceMappingType.BILL) {
                SettlementType.PINV
            } else {
                SettlementType.JVTDS
            }

            val settlementData = settlementRepository.findByDestIdAndDestType(paymentInvoiceMappingData.documentNo, destinationType)

            if (settlementData.any { it?.settlementStatus == SettlementStatus.POSTED }) {
                throw AresException(AresError.ERR_1541, "")
            }
            paymentRepository.deletePayment(payment.id)
            invoicePayMappingRepo.deletePaymentMappings(paymentInvoiceMappingData.id)
            createAudit(AresConstants.PAYMENTS, payment.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            createAudit("payment_invoice_map", paymentInvoiceMappingData.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)

            val settlementIds = settlementData.map { it?.id!! }
            settlementRepository.deleleSettlement(settlementIds)

            createAudit(AresConstants.SETTLEMENT, settlementIds[0], AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            if (settlementIds.size > 1) {
                createAudit(AresConstants.SETTLEMENT, settlementIds[1], AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            }

            val accountUtilizationPaymentData = accountUtilizationRepository.getDataByPaymentNum(payment.paymentNum)
            amountPaid = accountUtilizationPaymentData.payCurr
            ledTotalAmtPaid = accountUtilizationPaymentData.payLoc
            accountUtilizationRepository.deleteAccountUtilization(accountUtilizationPaymentData.id)

            val accUtilizationRecord = accountUtilization?.first { it.documentNo == paymentInvoiceMappingData.documentNo && it.accType.toString() == destinationType.toString() }

            if (paymentInvoiceMappingData.mappingType == PaymentInvoiceMappingType.TDS) {
                val jvRecord = journalVoucherRepository.findById(paymentInvoiceMappingData.documentNo)
                tdsPaid = jvRecord?.amount!!
                ledTdsPaid = jvRecord.ledAmount!!
                parentJVRepository.deleteJournalVoucherById(jvRecord.parentJvId!!, reverseUtrRequest.updatedBy!!)
                createAudit("parent_journal_vouchers", jvRecord.parentJvId, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
                journalVoucherRepository.deleteJvLineItemByParentJvId(jvRecord.parentJvId!!, reverseUtrRequest.updatedBy!!)
                createAudit("journal_vouchers", jvRecord.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
                accountUtilizationRepository.deleteAccountUtilization(accUtilizationRecord?.id!!)
                createAudit("account_utillizations", accUtilizationRecord.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            }
        }

        val accUtilizationRecord = accountUtilization?.first { it.documentNo == reverseUtrRequest.documentNo }

        var leftAmountPayCurr: BigDecimal? = accUtilizationRecord?.payCurr?.minus(amountPaid)?.minus(tdsPaid)
        var leftAmountLedgerCurr: BigDecimal? = accUtilizationRecord?.payLoc?.minus(ledTotalAmtPaid)?.minus(ledTdsPaid)

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

        if (leftAmountPayCurr != null) {
            paymentStatus = when {
                leftAmountPayCurr.compareTo(BigDecimal.ZERO) == 0 -> {
                    KnockOffStatus.UNPAID
                }
                leftAmountPayCurr.compareTo(accUtilizationRecord?.amountCurr) == 0 -> {
                    KnockOffStatus.FULL
                }
                else -> {
                    KnockOffStatus.PARTIAL
                }
            }
        }

        accountUtilizationRepository.updateAccountUtilization(accUtilizationRecord?.id!!, accUtilizationRecord.documentStatus!!, leftAmountPayCurr!!, leftAmountLedgerCurr!!)
        createAudit("account_utillizations", accUtilizationRecord.id, AresConstants.UPDATE, mapOf("pay_curr" to leftAmountPayCurr, "pay_loc" to leftAmountLedgerCurr), reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)

        aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accUtilizationRecord.organizationId))

        kuberMessagePublisher.emitPostRestoreUtr(
            restoreUtrResponse = RestoreUtrResponse(
                documentNo = reverseUtrRequest.documentNo,
                paidAmount = amountPaid,
                paidTds = BigDecimal.ZERO,
                paymentStatus = paymentStatus,
                paymentUploadAuditId = reverseUtrRequest.paymentUploadAuditId,
                updatedBy = reverseUtrRequest.updatedBy,
                performedByType = reverseUtrRequest.performedByType
            )
        )
        try {
            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = accUtilizationRecord.organizationId))
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

        for (index in indexToStop.until(settlementDetails.size)!!) {
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

    private suspend fun saveTdsAsJv(knockOffRecord: AccountPayablesFile, accountUtilization: AccountUtilization): JournalVoucher {
        var parentJournalVoucher = ParentJournalVoucher(
            id = null,
            status = JVStatus.POSTED,
            category = "JVTDS",
            jvNum = accountUtilization.documentValue,
            transactionDate = knockOffRecord.transactionDate,
            validityDate = knockOffRecord.transactionDate,
            currency = knockOffRecord.currency,
            ledCurrency = knockOffRecord.currency,
            entityCode = knockOffRecord.entityCode,
            exchangeRate = knockOffRecord.ledgerAmount.divide(knockOffRecord.currencyAmount)
                .setScale(AresConstants.DECIMAL_NUMBER_UPTO, RoundingMode.HALF_DOWN),
            description = "TDS AGAINST ${accountUtilization.documentValue}",
            createdBy = knockOffRecord.createdBy,
            updatedBy = knockOffRecord.updatedBy,
            jvCodeNum = "VTDS",
            isUtilized = true
        )

        parentJournalVoucher = parentJVRepository.save(parentJournalVoucher)

        val lineItemProps: MutableList<HashMap<String, Any?>> = mutableListOf(
            hashMapOf(
                "accMode" to "AP",
                "glCode" to "321000",
                "type" to "DEBIT",
                "tradePartyId" to knockOffRecord.organizationId.toString(),
                "tradePartyName" to knockOffRecord.organizationName,
                "signFlag" to -1
            ),
            hashMapOf(
                "accMode" to null,
                "glCode" to "324001",
                "type" to "CREDIT",
                "tradePartyId" to null,
                "tradePartyName" to null,
                "signFlag" to 1
            )
        )
        return saveJvLineItem(parentJournalVoucher, knockOffRecord, lineItemProps, accountUtilization)
    }

    private suspend fun saveJvLineItem(
        parentJvData: ParentJournalVoucher,
        knockOffRecord: AccountPayablesFile,
        jvLineItems: MutableList<HashMap<String, Any?>>,
        accountUtilization: AccountUtilization
    ): JournalVoucher {
        val jvLineItemData = jvLineItems.map { lineItem ->
            JournalVoucher(
                id = null,
                jvNum = parentJvData.jvNum!!,
                accMode = if (lineItem["accMode"] != null) AccMode.valueOf(lineItem["accMode"]!!.toString()) else AccMode.OTHER,
                category = parentJvData.category,
                createdAt = parentJvData.createdAt,
                createdBy = parentJvData.createdBy,
                updatedAt = parentJvData.createdAt,
                updatedBy = parentJvData.createdBy,
                currency = parentJvData.currency,
                ledCurrency = knockOffRecord.ledgerCurrency,
                amount = knockOffRecord.currencyAmount,
                ledAmount = knockOffRecord.ledgerAmount,
                description = parentJvData.description,
                entityCode = knockOffRecord.entityCode,
                entityId = UUID.fromString(AresConstants.ENTITY_ID[parentJvData.entityCode]),
                exchangeRate = parentJvData.exchangeRate,
                glCode = lineItem["glCode"].toString(),
                parentJvId = parentJvData.id,
                type = lineItem["type"].toString(),
                signFlag = lineItem["signFlag"]?.toString()?.toShort(),
                status = JVStatus.APPROVED,
                tradePartyId = UUID.fromString(lineItem["tradePartyId"].toString()),
                tradePartyName = lineItem["tradePartyName"].toString(),
                validityDate = parentJvData.transactionDate,
                migrated = false,
                deletedAt = null
            )
        }

        val jvLineItems = journalVoucherRepository.saveAll(jvLineItemData).toList()

        jvLineItems.filter { it.accMode != null && it.tradePartyId != null }.map {
            saveAccountUtilization(
                paymentNum = it.id!!,
                paymentNumValue = it.jvNum,
                knockOffRecord,
                accountUtilization,
                currTotalAmtPaid = it.amount!!,
                ledTotalAmtPaid = it.ledAmount!!,
                utilizedCurrTotalAmtPaid = it.amount,
                utilizedLedTotalAmtPaid = it.ledAmount,
                accountType = AccountType.JVTDS
            )
        }

        return jvLineItems.first()
    }
}
