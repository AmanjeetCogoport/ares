package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.payment.repository.PaymentNumGeneratorRepo
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class SequenceGeneratorImpl() {

    @Inject
    lateinit var paymentNumGeneratorRepo: PaymentNumGeneratorRepo

    suspend fun getPaymentNumber(sequenceType: String): Long {
        return paymentNumGeneratorRepo.getNextSequenceNumber(sequenceType)
    }
}
