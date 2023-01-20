package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.KuberMessagePublisher
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.entity.PaymentInvoiceMapping
import com.cogoport.ares.api.payment.mapper.PayableFileToPaymentMapper
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.InvoicePayMappingRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.settlement.entity.Settlement
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
import com.cogoport.ares.model.payment.PaymentInvoiceMappingType
import com.cogoport.ares.model.payment.RestoreUtrResponse
import com.cogoport.ares.model.payment.ReverseUtrRequest
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.AccountPayableFileResponse
import com.cogoport.ares.model.settlement.SettlementType
import io.sentry.Sentry
import jakarta.inject.Inject
import org.apache.kafka.common.KafkaException
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import javax.transaction.Transactional

open class KnockoffServiceImpl : KnockoffService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject
    lateinit var payableFileToPaymentMapper: PayableFileToPaymentMapper

    @Inject
    lateinit var aresKafkaEmitter: AresKafkaEmitter

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

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class], dontRollbackOn = [KafkaException::class])
    override suspend fun uploadBillPayment(knockOffRecord: AccountPayablesFile): AccountPayableFileResponse {

        /* CHECK INVOICE/BILL EXISTS IN ACCOUNT UTILIZATION FOR THAT KNOCK OFF DOCUMENT*/
        val accountUtilization = accountUtilizationRepository.findRecord(knockOffRecord.documentNo, knockOffRecord.accType.name, AccMode.AP.name)
        if (accountUtilization == null) {
            val accPayResponse = AccountPayableFileResponse(
                knockOffRecord.documentNo, knockOffRecord.documentValue, false,
                KnockOffStatus.UNPAID.name, Messages.NO_DOCUMENT_EXISTS,
                knockOffRecord.createdBy
            )
            emitPaymentStatus(accPayResponse)
            return accPayResponse
        }

        if (paymentRepository.isTransRefNumberExists(knockOffRecord.organizationId, knockOffRecord.transRefNumber)) {
            val accPayResponse = AccountPayableFileResponse(
                knockOffRecord.documentNo, knockOffRecord.documentValue, false,
                KnockOffStatus.UNPAID.name, Messages.NO_DOCUMENT_EXISTS,
                knockOffRecord.createdBy
            )
            emitPaymentStatus(accPayResponse)
            return accPayResponse
        }

        /*CREATE A NEW RECORD FOR THE PAYMENT TO VENDOR*/
        val paymentEntity = payableFileToPaymentMapper.convertToEntity(knockOffRecord)
        val savedPaymentRecord = savePayment(
            paymentEntity, isTDSEntry = false, knockOffRecord.createdBy.toString(), knockOffRecord.performedByType
        )

        /*UPDATE THE AMOUNT PAID IN THE EXISTING BILL IN ACCOUNT UTILIZATION*/
        var currTotalAmtPaid = knockOffRecord.currencyAmount + knockOffRecord.currTdsAmount
        var ledTotalAmtPaid = knockOffRecord.ledgerAmount + knockOffRecord.ledTdsAmount

        val isOverPaid = isOverPaid(accountUtilization, currTotalAmtPaid, ledTotalAmtPaid)

        if (isOverPaid) {
            accountUtilizationRepository.updateInvoicePayment(accountUtilization.id!!, accountUtilization.amountCurr - accountUtilization.payCurr, accountUtilization.amountLoc - accountUtilization.payLoc)
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

        saveSettlements(knockOffRecord, false, accountUtilization.documentNo, savedPaymentRecord.paymentNum, isOverPaid, accountUtilization)
        saveSettlements(knockOffRecord, true, accountUtilization.documentNo, savedPaymentRecord.paymentNum, isOverPaid, accountUtilization)

        /* SAVE THE ACCOUNT UTILIZATION FOR THE NEWLY PAYMENT DONE*/

        if (isOverPaid) {
            saveAccountUtilization(
                savedPaymentRecord.paymentNum!!, savedPaymentRecord.paymentNumValue!!, knockOffRecord, accountUtilization,
                currTotalAmtPaid, ledTotalAmtPaid, accountUtilization.amountCurr - accountUtilization.payCurr,
                accountUtilization.amountLoc - accountUtilization.payLoc
            )
        } else {
            saveAccountUtilization(
                savedPaymentRecord.paymentNum!!, savedPaymentRecord.paymentNumValue!!, knockOffRecord, accountUtilization,
                currTotalAmtPaid, ledTotalAmtPaid, currTotalAmtPaid, ledTotalAmtPaid
            )
        }

        /*SAVE THE PAYMENT DISTRIBUTION AGAINST THE INVOICE */
        saveInvoicePaymentMapping(savedPaymentRecord.id!!, knockOffRecord, isTDSEntry = false)

        /*IF TDS AMOUNT IS PRESENT  SAVE THE TDS SIMILARLY IN PAYMENT AND PAYMENT DISTRIBUTION*/
        if (knockOffRecord.currTdsAmount > BigDecimal.ZERO && knockOffRecord.ledTdsAmount > BigDecimal.ZERO) {
            paymentEntity.amount = knockOffRecord.currTdsAmount
            paymentEntity.ledAmount = knockOffRecord.ledTdsAmount

            val savedTDSPaymentRecord = savePayment(
                paymentEntity, isTDSEntry = true, knockOffRecord.createdBy.toString(), knockOffRecord.performedByType
            )
            saveInvoicePaymentMapping(savedTDSPaymentRecord.id!!, knockOffRecord, isTDSEntry = true)
        }

        var paymentStatus = KnockOffStatus.PARTIAL.name
        val leftAmount = accountUtilization.amountLoc - (accountUtilization.payLoc + ledTotalAmtPaid)

        if (leftAmount <= 0.toBigDecimal() || leftAmount.setScale(2, RoundingMode.HALF_UP) <= 0.toBigDecimal())
            paymentStatus = KnockOffStatus.FULL.name

        var accPayResponse = AccountPayableFileResponse(knockOffRecord.documentNo, knockOffRecord.documentValue, true, paymentStatus, null, knockOffRecord.createdBy)
        try {
            emitPaymentStatus(accPayResponse)
            aresMessagePublisher.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = knockOffRecord.organizationId))
        } catch (k: KafkaException) {
            logger().error(k.stackTraceToString())
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
            Sentry.captureException(e)
        }
        return accPayResponse
    }

    private fun isOverPaid(accountUtilization: AccountUtilization, currTotalAmtPaid: BigDecimal, ledTotalAmtPaid: BigDecimal): Boolean {
        if (accountUtilization.amountCurr < accountUtilization.payCurr + currTotalAmtPaid && accountUtilization.amountLoc < accountUtilization.payLoc + ledTotalAmtPaid)
            return true
        return false
    }
    /**
     * Emits Kafka message on topic <b>payables-bill-status</b>
     * @param : accPayResponseList
     */
    private fun emitPaymentStatus(accPayResponseList: AccountPayableFileResponse) {
        var event = com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent(accPayResponseList)
        aresKafkaEmitter.emitBillPaymentStatus(event)
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
            paymentEntity.paymentNumValue = SequenceSuffix.PAYMENT.prefix + paymentEntity.paymentNum
        }
        paymentEntity.migrated = false
        /* CREATE A NEW RECORD FOR THE PAYMENT AND SAVE THE PAYMENT IN DATABASE*/
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

    private suspend fun saveInvoicePaymentMapping(paymentId: Long, knockOffRecord: AccountPayablesFile, isTDSEntry: Boolean) {
        var invoicePayMap = PaymentInvoiceMapping(
            id = null,
            accountMode = AccMode.AP,
            documentNo = knockOffRecord.documentNo,
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
            accType = SignSuffix.PAY.accountType,
            accMode = knockOffRecord.accMode,
            signFlag = SignSuffix.PAY.sign,
            currency = knockOffRecord.currency,
            ledCurrency = knockOffRecord.ledgerCurrency,
            amountCurr = currTotalAmtPaid,
            amountLoc = ledTotalAmtPaid,
            payCurr = utilizedCurrTotalAmtPaid,
            payLoc = utilizedLedTotalAmtPaid,
            taxableAmount = BigDecimal.ZERO,
            dueDate = accountUtilization.dueDate,
            transactionDate = knockOffRecord.transactionDate,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now()),
            orgSerialId = knockOffRecord.orgSerialId,
            migrated = false
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
    }

    private suspend fun saveSettlements(
        knockOffRecord: AccountPayablesFile,
        isTDSEntry: Boolean,
        destinationId: Long?,
        sourceId: Long?,
        isOverPaid: Boolean,
        accountUtilization: AccountUtilization
    ) {

        val settlement = generateSettlementEntity(knockOffRecord, isTDSEntry, destinationId, sourceId, isOverPaid, accountUtilization)
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
    }

    private fun generateSettlementEntity(
        knockOffRecord: AccountPayablesFile,
        isTDSEntry: Boolean,
        destinationId: Long?,
        sourceId: Long?,
        isOverPaid: Boolean,
        accountUtilization: AccountUtilization
    ): Settlement {
        var ledAmount: BigDecimal
        var amount: BigDecimal
        if (isTDSEntry) {
            ledAmount = knockOffRecord.ledTdsAmount
            amount = knockOffRecord.currTdsAmount
        } else {
            if (isOverPaid) {
                ledAmount = accountUtilization.amountLoc - accountUtilization.payLoc
                amount = accountUtilization.amountCurr - accountUtilization.payCurr
            } else {
                ledAmount = knockOffRecord.ledgerAmount
                amount = knockOffRecord.currencyAmount
            }
        }
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
            settlementDate = Date(Timestamp.from(Instant.now()).time)
        )
    }
    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun reverseUtr(reverseUtrRequest: ReverseUtrRequest) {
        val accountUtilization = accountUtilizationRepository.findRecord(reverseUtrRequest.documentNo, AccountType.PINV.name, AccMode.AP.name)
        val payments = paymentRepository.findByTransRef(reverseUtrRequest.transactionRef)
        var tdsPaid = 0.toBigDecimal()
        var ledTdsPaid = 0.toBigDecimal()
        var amountPaid: BigDecimal = 0.toBigDecimal()
        var ledTotalAmtPaid: BigDecimal = 0.toBigDecimal()

        for (payment in payments) {
            val paymentInvoiceMappingData = invoicePayMappingRepo.findByPaymentId(reverseUtrRequest.documentNo, payment.id)
            paymentRepository.deletePayment(payment.id)

            if (paymentInvoiceMappingData.mappingType == PaymentInvoiceMappingType.BILL) {
                amountPaid = paymentInvoiceMappingData.amount
                ledTotalAmtPaid = paymentInvoiceMappingData.ledAmount
            } else if (paymentInvoiceMappingData.mappingType == PaymentInvoiceMappingType.TDS) {
                tdsPaid = paymentInvoiceMappingData.amount
                ledTdsPaid = paymentInvoiceMappingData.ledAmount
            }
            invoicePayMappingRepo.deletePaymentMappings(paymentInvoiceMappingData.id)
            createAudit(AresConstants.PAYMENTS, payment.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
            createAudit("payment_invoice_map", paymentInvoiceMappingData.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        }

        val settlementIds = settlementRepository.getSettlementByDestinationId(reverseUtrRequest.documentNo, payments[0]?.paymentNum!!)
        settlementRepository.deleleSettlement(settlementIds)

        createAudit(AresConstants.SETTLEMENT, settlementIds[0], AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        createAudit(AresConstants.SETTLEMENT, settlementIds[1], AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        val accountUtilizationPaymentData = accountUtilizationRepository.getDataByPaymentNum(payments[0]?.paymentNum)
        accountUtilizationRepository.deleteAccountUtilization(accountUtilizationPaymentData.id)
        var leftAmountPayCurr: BigDecimal? = accountUtilization?.payCurr?.minus(accountUtilizationPaymentData.payCurr)
        var leftAmountLedgerCurr: BigDecimal? = accountUtilization?.payLoc?.minus(accountUtilizationPaymentData.payLoc)

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
        accountUtilizationRepository.updateAccountUtilization(accountUtilization?.id!!, leftAmountPayCurr!!, leftAmountLedgerCurr!!)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilizationPaymentData.id, AresConstants.DELETE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)
        createAudit(AresConstants.ACCOUNT_UTILIZATIONS, accountUtilization?.id!!, AresConstants.UPDATE, null, reverseUtrRequest.updatedBy.toString(), reverseUtrRequest.performedByType)

        kuberMessagePublisher.emitPostRestoreUtr(
            restoreUtrResponse = RestoreUtrResponse(
                documentNo = reverseUtrRequest.documentNo,
                paidAmount = amountPaid,
                paidTds = tdsPaid,
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
}
