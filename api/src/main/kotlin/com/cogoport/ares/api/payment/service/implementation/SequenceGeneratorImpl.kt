package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.payment.repository.PaymentNumGeneratorRepo
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate

@Singleton
class SequenceGeneratorImpl() {

    @Inject
    lateinit var paymentNumGeneratorRepo: PaymentNumGeneratorRepo

    suspend fun getPaymentNumber(sequenceType: String): Long {
        return paymentNumGeneratorRepo.getNextSequenceNumber(sequenceType)
    }
    suspend fun getSettlementNumber(): String {
        val prefix = SequenceSuffix.SETTLEMENT.prefix
        val number = paymentNumGeneratorRepo.getNextSequenceNumber(prefix)
        return "${prefix}${ LocalDate.now().year}00000000$number"
    }
}
