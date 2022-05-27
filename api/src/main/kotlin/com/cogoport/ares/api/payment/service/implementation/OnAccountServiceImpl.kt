package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.model.payment.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDateTime
import javax.transaction.Transactional

@Singleton
class OnAccountServiceImpl : OnAccountService {
    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    /**
     * Fetch Account Collection payments from DB.
     * @param : updatedDate, entityType, currencyType
     * @return : AccountCollectionResponse
     */
    override suspend fun getOnAccountCollections(
        uploadedDate: LocalDateTime?,
        entityType: Int?,
        currencyType: String?
    ): AccountCollectionResponse {
        var payments = mutableListOf<Payment>()
        var data = paymentRepository.listOrderByCreatedAtDesc()
        data.forEach {
            val payment = paymentConverter.convertToModel(it)
            payments.add(payment)
        }
        return AccountCollectionResponse(payments = payments)
    }

    @Transactional(rollbackOn = [Exception::class])
    override open suspend fun createPaymentEntry(receivableRequest: Payment): Payment {

        var payment = paymentConverter.convertToEntity(receivableRequest)
        paymentRepository.save(payment)

        var accountUtilization = AccountUtilization(
            id = null,
            documentNo = payment.id!!,
                documentValue = "invoice",
                zoneCode = "NORTH",
                serviceType = "FCL_FREIGHT",
                docStatus = "FINAL",
                entityCode = payment.entityCode,
                category = "asset",
            orgSerialId = payment.orgSerialId!!,
                sageOrganizationId = payment.sageOrganizationId,
            organizationId = payment.organizationId,
            organizationName = payment.organizationName,
            accCode = payment.accCode,
            accType = AccountType.REC,
            accMode = payment.accMode,
            signFlag = payment.signFlag,
            currency = payment.currency,
                ledCurrency = payment.currency,
            amountCurr = payment.amount,
            amountLoc = payment.ledAmount,
            dueDate = payment.transactionDate!!,
            transactionDate = payment.transactionDate!!
        )

        accountUtilizationRepository.save(accountUtilization)
        return paymentConverter.convertToModel(payment)
    }


    override suspend fun updatePaymentEntry(receivableRequest: Payment): Payment? {
        var payment = receivableRequest.id?.let { paymentRepository.findById(it) }
        var accountUtilization = accountUtilizationRepository.findByPaymentId(receivableRequest.id)
        if (payment != null && payment.isPosted && accountUtilization != null)
            throw AresException(AresError.ERR_1006, "")

        return updatePayment(receivableRequest, accountUtilization, payment)
    }

    @Transactional(rollbackOn = [Exception::class, AresException::class])
    open suspend fun updatePayment(receivableRequest: Payment, accountUtilization: AccountUtilization, payment: com.cogoport.ares.api.payment.entity.Payment?): Payment? {
        paymentRepository.update(paymentConverter.convertToEntity(receivableRequest))
        accountUtilizationRepository.update(updateAccountUtilizationEntry(accountUtilization, receivableRequest))
//        return payment?.let { paymentConverter.convertToModel(it) }
        var payment = receivableRequest.id?.let { paymentRepository.findById(it) }
        return payment?.let { paymentConverter.convertToModel(it) }
    }

//    override suspend fun updatePostOnPaymentEntry(paymentId: Long): Long? {
//        try {
//            var payment: com.cogoport.ares.api.payment.entity.Payment = paymentRepository.findById(paymentId) ?: throw AresException(AresError.ERR_1001, "")
//
//            if (payment.isPosted)
//                throw AresException(AresError.ERR_1001, "")
//
//            payment.isPosted = true
//            paymentRepository.update(payment)
//            return payment.id
//        } catch (e: Exception) {
//            throw e
//        }
//    }

    override suspend fun deletePaymentEntry(paymentId: Long): String? {
        try {

            var payment: com.cogoport.ares.api.payment.entity.Payment = paymentRepository.findById(paymentId) ?: throw AresException(AresError.ERR_1001, "")

            if (payment.isDeleted)
                throw AresException(AresError.ERR_1001, "")

            payment.isDeleted = true
            paymentRepository.update(payment)
            return "Successfully Deleted!"
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun upload(): Boolean {
        TODO("Not yet implemented")
    }

    suspend fun updateAccountUtilizationEntry(accountUtilization: AccountUtilization, receivableRequest: Payment): AccountUtilization {
        accountUtilization.zoneCode = "NORTH"
        accountUtilization.docStatus = "FINAL"
        accountUtilization.serviceType = "FCL_FREIGHT"
        accountUtilization.entityCode = receivableRequest.entityType
        accountUtilization.category = "non_asset"
        accountUtilization.orgSerialId = receivableRequest.orgSerialId!!
        accountUtilization.organizationId = receivableRequest.customerId
        accountUtilization.organizationName = receivableRequest.customerName
        accountUtilization.sageOrganizationId = receivableRequest.sageOrganizationId
        accountUtilization.accCode = receivableRequest.accCode
        accountUtilization.accMode = receivableRequest.accMode
        accountUtilization.signFlag = receivableRequest.signFlag
        accountUtilization.amountCurr = receivableRequest.amount
        accountUtilization.amountLoc = receivableRequest.ledAmount
        accountUtilization.dueDate = receivableRequest.transactionDate!!
        accountUtilization.transactionDate = receivableRequest.transactionDate!!
        return accountUtilization
    }

    suspend fun passedAllValidation(payment: Payment) : Boolean{
        TODO("Not implemented")
    }

    suspend fun checkForNumeral(value: String) : Boolean{
        var pattern = Regex("[+-]?[0-9]+(\\.[0-9]+)?([Ee][+-]?[0-9]+)?")
        return pattern.matches(value.replace(",",""))
    }
    suspend fun checkForCurrency(value: String) : Boolean {
        return AllCurrencyTypes.values().any{ it.name == value}
    }

    suspend fun checkForPayMode(value: String) : Boolean {
        return PayMode.values().any{ it.name == value}
    }

    suspend fun checkForCustomerName(value: String) : Boolean {
        var pattern = Regex("^[\\p{L} .'-]+$")
        return pattern.matches(value)
    }

    suspend fun validateUTR(value: String) : Boolean {
        var pattern = Regex("[a-zA-Z0-9]*")
        return pattern.matches(value) && value.length > 10 && value.length < 20
    }

    suspend fun checkLocalDate(value: String) : Boolean {
        var pattern = Regex("^(20[0-9]{2})-(1[0-2]|0[1-9])-(3[01]|[12][0-9]|0[1-9])$")
        return pattern.matches(value)
    }

}
