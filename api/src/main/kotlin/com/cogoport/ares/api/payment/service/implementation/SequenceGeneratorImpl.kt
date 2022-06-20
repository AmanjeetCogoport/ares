package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.payment.repository.PaymentNumGeneratorRepo
import io.micronaut.context.annotation.Context
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
@Context
class SequenceGeneratorImpl() {

    @Inject
    lateinit var paymentNumGeneratorRepo: PaymentNumGeneratorRepo

    suspend fun getPaymentNumber(sequenceType: String): Long {
        return paymentNumGeneratorRepo.getNextSequenceNumber(sequenceType)
    }
}
