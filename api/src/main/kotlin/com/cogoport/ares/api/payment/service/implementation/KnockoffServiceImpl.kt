package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.mapper.PayableFileToAccountUtilMapper
import com.cogoport.ares.api.payment.mapper.PayableFileToPaymentMapper
import com.cogoport.ares.api.payment.model.AccountPayableFileResponse
import com.cogoport.ares.api.payment.model.AccountPayablesFile
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.PayMode
import jakarta.inject.Inject
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

            var accMode = AccMode.valueOf(knockOffRecord.accMode)
            var paymentMode = PayMode.valueOf(knockOffRecord.paymentMode!!)
            var accType = AccountType.valueOf(knockOffRecord.accType)

            /*1.Check if invoice exists for that record in account_utilizations table*/
            var accountUtilization = accountUtilizationRepository.findRecord(knockOffRecord.documentNo, knockOffRecord.accType)

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

            /*2. Save the payment record*/
            paymentRepository.save(paymentEntity)

            /*3. Update the existing invoice with the payment received*/
            accountUtilizationRepository.updateInvoicePayment(
                accountUtilization.id!!,
                paymentEntity.amount,
                paymentEntity.ledAmount
            )

            /*4. Insert the account utilization record*/
            var accountUtilEntity = payableFileToAccountUtilMapper.convertToEntity(knockOffRecord)

            accountUtilizationRepository.save(accountUtilEntity)

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
