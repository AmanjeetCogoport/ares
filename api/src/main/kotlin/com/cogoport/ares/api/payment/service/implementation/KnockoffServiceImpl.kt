package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.enums.SignSuffix
import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.entity.PaymentInvoiceMapping
import com.cogoport.ares.api.payment.mapper.PayableFileToPaymentMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.InvoicePayMappingRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.common.KnockOffStatus
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountPayableFileResponse
import com.cogoport.ares.model.payment.AccountPayablesFile
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentInvoiceMappingType
import jakarta.inject.Inject
import org.apache.kafka.common.KafkaException
import java.math.BigDecimal
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import javax.transaction.Transactional

open class KnockoffServiceImpl : KnockoffService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var payableFileToPaymentMapper: PayableFileToPaymentMapper

    @Inject
    lateinit var aresKafkaEmitter: AresKafkaEmitter

    @Inject
    lateinit var invoicePayMappingRepo: InvoicePayMappingRepository

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class], dontRollbackOn = [KafkaException::class])
    override suspend fun uploadBillPayment(knockOffRecord: AccountPayablesFile): AccountPayableFileResponse {

        /* CHECK INVOICE/BILL EXISTS IN ACCOUNT UTILIZATION FOR THAT KNOCK OFF DOCUMENT*/
        val accountUtilization = accountUtilizationRepository.findRecord(knockOffRecord.documentNo, knockOffRecord.accType.name, AccMode.AP.name)
        if (accountUtilization == null) {
            val accPayResponse = AccountPayableFileResponse(
                knockOffRecord.documentNo, knockOffRecord.documentValue, false,
                KnockOffStatus.UNPAID.name, Messages.NO_DOCUMENT_EXISTS
            )
            emitPaymentStatus(accPayResponse)
            return accPayResponse
        }

        if (paymentRepository.isTransRefNumberExists(knockOffRecord.organizationId, knockOffRecord.transRefNumber)) {
            val accPayResponse = AccountPayableFileResponse(
                knockOffRecord.documentNo, knockOffRecord.documentValue, false,
                KnockOffStatus.UNPAID.name, Messages.NO_DOCUMENT_EXISTS
            )
            emitPaymentStatus(accPayResponse)
            return accPayResponse
        }

        /*CREATE A NEW RECORD FOR THE PAYMENT TO VENDOR*/
        val paymentEntity = payableFileToPaymentMapper.convertToEntity(knockOffRecord)
        val savedPaymentRecord = savePayment(paymentEntity, isTDSEntry = false)

        /*UPDATE THE AMOUNT PAID IN THE EXISTING BILL IN ACCOUNT UTILIZATION*/
        val currTotalAmtPaid = knockOffRecord.currencyAmount + knockOffRecord.currTdsAmount
        val ledTotalAmtPaid = knockOffRecord.ledgerAmount + knockOffRecord.ledTdsAmount
        accountUtilizationRepository.updateInvoicePayment(accountUtilization.id!!, currTotalAmtPaid, ledTotalAmtPaid)

        /* SAVE THE ACCOUNT UTILIZATION FOR THE NEWLY PAYMENT DONE*/
        saveAccountUtilization(
            savedPaymentRecord.paymentNum!!, savedPaymentRecord.paymentNumValue!!, knockOffRecord, accountUtilization,
            currTotalAmtPaid, ledTotalAmtPaid
        )

        /*SAVE THE PAYMENT DISTRIBUTION AGAINST THE INVOICE */
        saveInvoicePaymentMapping(savedPaymentRecord.id!!, knockOffRecord, isTDSEntry = false)

        /*IF TDS AMOUNT IS PRESENT  SAVE THE TDS SIMILARLY IN PAYMENT AND PAYMENT DISTRIBUTION*/
        if (knockOffRecord.currTdsAmount > BigDecimal.ZERO && knockOffRecord.ledTdsAmount > BigDecimal.ZERO) {
            paymentEntity.amount = knockOffRecord.currTdsAmount
            paymentEntity.ledAmount = knockOffRecord.ledTdsAmount

            val savedTDSPaymentRecord = savePayment(paymentEntity, isTDSEntry = true)
            saveInvoicePaymentMapping(savedTDSPaymentRecord.id!!, knockOffRecord, isTDSEntry = true)
        }

        var paymentStatus = KnockOffStatus.PARTIAL.name
        val leftAmount = accountUtilization.amountLoc - (accountUtilization.payLoc + ledTotalAmtPaid)

        if (leftAmount.compareTo(BigDecimal.ZERO) == 0)
            paymentStatus = KnockOffStatus.FULL.name

        var accPayResponse = AccountPayableFileResponse(knockOffRecord.documentNo, knockOffRecord.documentValue, true, paymentStatus, null)
        try {
            emitPaymentStatus(accPayResponse)
        } catch (k: KafkaException) {
            logger().error(k.stackTraceToString())
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
        return accPayResponse
    }

    /**
     * Emits Kafka message on topic <b>payables-bill-status</b>
     * @param : accPayResponseList
     */
    private fun emitPaymentStatus(accPayResponseList: AccountPayableFileResponse) {
        var event = com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent(accPayResponseList)
        aresKafkaEmitter.emitBillPaymentStatus(event)
    }

    private suspend fun savePayment(paymentEntity: Payment, isTDSEntry: Boolean): Payment {
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
        /* CREATE A NEW RECORD FOR THE PAYMENT AND SAVE THE PAYMENT IN DATABASE*/
        return paymentRepository.save(paymentEntity)
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
        ledTotalAmtPaid: BigDecimal
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
            organizationName = knockOffRecord.organizationName,
            accCode = AresModelConstants.AP_ACCOUNT_CODE,
            accType = knockOffRecord.accType,
            accMode = knockOffRecord.accMode,
            signFlag = knockOffRecord.signFlag,
            currency = knockOffRecord.currency,
            ledCurrency = knockOffRecord.ledgerCurrency,
            amountCurr = currTotalAmtPaid,
            amountLoc = ledTotalAmtPaid,
            payCurr = currTotalAmtPaid,
            payLoc = ledTotalAmtPaid,
            taxableAmount = BigDecimal.ZERO,
            dueDate = accountUtilization.dueDate,
            transactionDate = knockOffRecord.transactionDate,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now()),
            orgSerialId = knockOffRecord.orgSerialId
        )
        accountUtilizationRepository.save(accountUtilEntity)
    }
}
