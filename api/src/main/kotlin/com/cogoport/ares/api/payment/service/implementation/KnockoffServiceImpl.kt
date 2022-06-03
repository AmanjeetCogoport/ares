package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.PayableFileToAccountUtilMapper
import com.cogoport.ares.api.payment.mapper.PayableFileToPaymentMapper
import com.cogoport.ares.api.payment.model.AccountPayableFileResponse
import com.cogoport.ares.api.payment.model.AccountPayablesFile
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PaymentCode
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import jakarta.inject.Inject
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import javax.transaction.Transactional

open class KnockoffServiceImpl : KnockoffService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var payableFileToPaymentMapper: PayableFileToPaymentMapper

    @Inject
    lateinit var payableFileToAccountUtilMapper: PayableFileToAccountUtilMapper

    @Transactional(Transactional.TxType.REQUIRES_NEW)
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
                                knockOffRecord.documentNo, knockOffRecord.documentValue, false, Messages.NO_DOCUMENT_EXISTS
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

            /*4. Insert the account utilization record for payments*/
            var accountUtilEntity = AccountUtilization(id = null, documentNo = paymentId!!, documentValue = paymentId.toString(),
                    zoneCode = knockOffRecord.zoneCode, serviceType = accountUtilization.serviceType, documentStatus = DocumentStatus.FINAL,
                    entityCode = knockOffRecord.entityCode, category = knockOffRecord.category, sageOrganizationId = null,
                    organizationId = knockOffRecord.organizationId!!, organizationName = knockOffRecord.organizationName,
                    accCode = AresModelConstants.AP_ACCOUNT_CODE, accType = knockOffRecord.accType, accMode = knockOffRecord.accMode,
                    signFlag = knockOffRecord.signFlag, currency = knockOffRecord.currency, ledCurrency = knockOffRecord.ledgerCurrency,
                    amountCurr = currTotalAmtPaid, amountLoc = ledTotalAmtPaid, payCurr = currTotalAmtPaid, payLoc = ledTotalAmtPaid,
                    dueDate = accountUtilization.dueDate, transactionDate = knockOffRecord.transactionDate, createdAt = Timestamp.from(Instant.now()),
                    updatedAt = Timestamp.from(Instant.now()), orgSerialId = 0)

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
            }
            // Add success in the return response
            uploadBillResponseList.add(
                    AccountPayableFileResponse(
                            knockOffRecord.documentNo, knockOffRecord.documentValue,
                            true, null
                    )
            )
        }
        return uploadBillResponseList
    }
}
