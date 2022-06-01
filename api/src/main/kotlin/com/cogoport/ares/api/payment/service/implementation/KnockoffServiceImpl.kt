package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.model.AccountPayableFileResponse
import com.cogoport.ares.api.payment.model.AccountPayablesFile
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.common.models.Messages
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PayMode
import jakarta.inject.Inject
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.transaction.Transactional

open class KnockoffServiceImpl : KnockoffService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    override suspend fun uploadBillPayment(knockOffList: List<AccountPayablesFile>): MutableList<AccountPayableFileResponse> {

        var uploadBillResponseList = mutableListOf<AccountPayableFileResponse>()

        if (knockOffList.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1003, "knockOffList")
        }

        for (knockOffRecord in knockOffList) {

            var accMode = AccMode.valueOf(knockOffRecord.accMode)
            var paymentMode = PayMode.valueOf(knockOffRecord.paymentMode!!)
            var accType = AccountType.valueOf(knockOffRecord.accType)

            /*1.Check if invoice exists for that record in account_utilizations table*/
            var accountUtilizationId = accountUtilizationRepository.getAccountUtilizationId(knockOffRecord.documentNo, knockOffRecord.accType)

            if (accountUtilizationId.equals(0)) {
                // Add error that this invoice cannot be processed due to invoice does not exists
                uploadBillResponseList.add(
                    AccountPayableFileResponse(
                        knockOffRecord.documentNo, knockOffRecord.documentValue, false, Messages.NO_DOCUMENT_EXISTS
                    )
                )
                continue
            }

            var paymentEntity = Payment(
                null,
                knockOffRecord.entityCode,
                null,
                knockOffRecord.orgSerialId,
                knockOffRecord.organizationId!!,
                knockOffRecord.organizationName!!,
                knockOffRecord.sageOrganizationId!!,
                knockOffRecord.accCode,
                accMode,
                knockOffRecord.signFlag,
                knockOffRecord.currency,
                knockOffRecord.currencyAmount,
                knockOffRecord.ledgerCurrency,
                knockOffRecord.ledgerAmount,
                paymentMode,
                knockOffRecord.narration!!,
                knockOffRecord.transRefNumber,
                null,
                knockOffRecord.transactionDate,
                knockOffRecord.isPosted,
                false,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                knockOffRecord.accountNo!!
            )

            /*2. Save the payment record*/
            paymentRepository.save(paymentEntity)

            /*3. Update the existing invoice with the payment received*/
            accountUtilizationRepository.updateInvoicePayment(
                accountUtilizationId,
                paymentEntity.amount,
                paymentEntity.ledAmount
            )

            /*4. Insert the account utilization record*/
            var accountUtilEntity = AccountUtilization(
                null,
                knockOffRecord.documentNo,
                knockOffRecord.documentValue,
                knockOffRecord.zoneCode,
                knockOffRecord.serviceType,
                DocumentStatus.valueOf(knockOffRecord.documentStatus),
                knockOffRecord.entityCode,
                knockOffRecord.category,
                knockOffRecord.orgSerialId,
                knockOffRecord.sageOrganizationId,
                knockOffRecord.organizationId!!,
                knockOffRecord.organizationName,
                knockOffRecord.accCode,
                accType,
                accMode,
                knockOffRecord.signFlag,
                knockOffRecord.currency,
                knockOffRecord.ledgerCurrency,
                knockOffRecord.currencyAmount,
                knockOffRecord.ledgerAmount,
                knockOffRecord.currencyAmount,
                knockOffRecord.ledgerAmount,
                knockOffRecord.transactionDate,
                knockOffRecord.transactionDate,
                null,
                null
            )

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
