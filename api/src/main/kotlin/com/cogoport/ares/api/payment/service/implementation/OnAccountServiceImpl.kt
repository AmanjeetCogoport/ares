package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDateTime

@Singleton
class OnAccountServiceImpl : OnAccountService {
    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var paymentConverter: PaymentToPaymentMapper

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

    /**
     * Create new Payment entry in DB.
     * @param : com.cogoport.ares.receivables.model.Payment
     * @return : com.cogoport.plutus.receivables.model.Payment
     */
    override suspend fun createReceivables(receivableRequest: Payment): Payment {
        var payment = paymentRepository.save(
            paymentConverter.convertToEntity(receivableRequest)
        )
        return paymentConverter.convertToModel(payment)
    }
    override suspend fun upload(): Boolean {
        TODO("Not yet implemented")
    }
}
