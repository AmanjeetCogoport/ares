package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.exception.AresError
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

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class], dontRollbackOn = [KafkaException::class])
    override suspend fun uploadBillPayment(knockOffList: List<AccountPayablesFile>): MutableList<AccountPayableFileResponse> {

        var uploadBillResponseList = mutableListOf<AccountPayableFileResponse>()

        if (knockOffList.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1003, " knockOffList")
        }
        for (knockOffRecord in knockOffList) {
            /*1.Check if invoice exists for that record in account_utilizations table*/
            var accountUtilization = accountUtilizationRepository.findRecord(knockOffRecord.documentNo)

            if (accountUtilization == null) {
                // Add error that this invoice cannot be processed due to invoice does not exists
                uploadBillResponseList.add(
                        AccountPayableFileResponse(
                                knockOffRecord.documentNo, knockOffRecord.documentValue, false, "UNPAID", Messages.NO_DOCUMENT_EXISTS
                        )
                )
                continue
            }

            var paymentEntity = payableFileToPaymentMapper.convertToEntity(knockOffRecord)
            paymentEntity.paymentCode = PaymentCode.PAY // Payment Code is Pay as it is a bill payment
            paymentEntity.accCode = AresModelConstants.AP_ACCOUNT_CODE
            paymentEntity.createdAt = Timestamp.from(Instant.now())
            paymentEntity.updatedAt = Timestamp.from(Instant.now())
            /*2. Save the payment record*/
            val paymentId = paymentRepository.save(paymentEntity).id

            /*3. Update the existing invoice with the payment received*/
            val currTotalAmtPaid = knockOffRecord.currencyAmount + knockOffRecord.currTdsAmount
            val ledTotalAmtPaid = knockOffRecord.ledgerAmount + knockOffRecord.ledTdsAmount

            accountUtilizationRepository.updateInvoicePayment(
                    accountUtilization.id!!,
                    currTotalAmtPaid,
                    ledTotalAmtPaid
            )

            /*4. Add the record in invoice payment mapping*/
            var invoicePayMap = PaymentInvoiceMapping(
                    id = null,
                    accountMode = AccMode.AP,
                    documentNo = knockOffRecord.documentNo,
                    paymentId = paymentId!!,
                    mappingType = PaymentInvoiceMappingType.BILL.name,
                    currency = knockOffRecord.currency,
                    ledCurrency = knockOffRecord.ledgerCurrency,
                    signFlag = -1,
                    amount = knockOffRecord.currencyAmount,
                    ledAmount = knockOffRecord.ledgerAmount,
                    transactionDate = knockOffRecord.transactionDate,
                    createdAt = Timestamp.from(Instant.now()),
                    updatedAt = Timestamp.from(Instant.now())
            )
            invoicePayMappingRepo.save(invoicePayMap)

            /*4. Insert the account utilization record for payments*/
            var accountUtilEntity = AccountUtilization(
                    id = null, documentNo = paymentId!!, documentValue = paymentId.toString(),
                    zoneCode = knockOffRecord.zoneCode.toString(), serviceType = accountUtilization.serviceType, documentStatus = DocumentStatus.FINAL,
                    entityCode = knockOffRecord.entityCode, category = knockOffRecord.category, sageOrganizationId = null,
                    organizationId = knockOffRecord.organizationId!!, organizationName = knockOffRecord.organizationName,
                    accCode = AresModelConstants.AP_ACCOUNT_CODE, accType = knockOffRecord.accType, accMode = knockOffRecord.accMode,
                    signFlag = knockOffRecord.signFlag, currency = knockOffRecord.currency, ledCurrency = knockOffRecord.ledgerCurrency,
                    amountCurr = currTotalAmtPaid, amountLoc = ledTotalAmtPaid, payCurr = currTotalAmtPaid, payLoc = ledTotalAmtPaid,
                    dueDate = accountUtilization.dueDate, transactionDate = knockOffRecord.transactionDate, createdAt = Timestamp.from(Instant.now()),
                    updatedAt = Timestamp.from(Instant.now()), orgSerialId = knockOffRecord.orgSerialId
            )
            accountUtilizationRepository.save(accountUtilEntity)

            /*5. If TDS Amount is present add a TDS entry in payment table*/
            if (knockOffRecord.currTdsAmount > BigDecimal.ZERO && knockOffRecord.ledTdsAmount > BigDecimal.ZERO) {
                var payTdsEntity = payableFileToPaymentMapper.convertToEntity(knockOffRecord)
                payTdsEntity.paymentCode = PaymentCode.VTDS // Vendor TDS Payment code
                payTdsEntity.accCode = AresModelConstants.TDS_AP_ACCOUNT_CODE
                payTdsEntity.amount = knockOffRecord.currTdsAmount
                payTdsEntity.ledAmount = knockOffRecord.ledTdsAmount
                payTdsEntity.createdAt = Timestamp.from(Instant.now())
                payTdsEntity.updatedAt = Timestamp.from(Instant.now())
                paymentRepository.save(payTdsEntity)

                /*4. Add the record in invoice payment mapping for TDS*/
                var invoicePayTdsMap = PaymentInvoiceMapping(
                        id = null,
                        accountMode = AccMode.AP,
                        documentNo = knockOffRecord.documentNo,
                        paymentId = paymentId!!,
                        mappingType = PaymentInvoiceMappingType.TDS.name,
                        currency = knockOffRecord.currency,
                        ledCurrency = knockOffRecord.ledgerCurrency,
                        signFlag = -1,
                        amount = knockOffRecord.currTdsAmount,
                        ledAmount = knockOffRecord.ledTdsAmount,
                        transactionDate = knockOffRecord.transactionDate,
                        createdAt = Timestamp.from(Instant.now()),
                        updatedAt = Timestamp.from(Instant.now())
                )
                invoicePayMappingRepo.save(invoicePayTdsMap)
            }
            var paymentStatus = accountUtilizationRepository.findDocumentStatus(knockOffRecord.documentNo, AccMode.AP.name)
            var accPayResponse = AccountPayableFileResponse(
                    knockOffRecord.documentNo, knockOffRecord.documentValue,
                    true, paymentStatus, null
            )
            try {
                emitPaymentStatus(accPayResponse)
            } catch (k: KafkaException) {
                logger().error(k.stackTraceToString())
            } catch (e: Exception) {
                logger().error(e.stackTraceToString())
            }
            // Add success in the return response
            uploadBillResponseList.add(accPayResponse)
        }

        return uploadBillResponseList
    }

    /**
     * Emits Kafka message on topic <b>payables-bill-status</b>
     * @param : accPayResponseList
     */
    private fun emitPaymentStatus(accPayResponseList: AccountPayableFileResponse) {
        var event = com.cogoport.ares.model.payment.event.PayableKnockOffProduceEvent(accPayResponseList)
        aresKafkaEmitter.emitBillPaymentStatus(event)
    }
}
