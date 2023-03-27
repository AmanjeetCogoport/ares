package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.payment.repository.PaymentNumGeneratorRepo
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDateTime

@Singleton
class SequenceGeneratorImpl() {

    @Inject
    lateinit var paymentNumGeneratorRepo: PaymentNumGeneratorRepo

    suspend fun getPaymentNumber(sequenceType: String): Long {
        return paymentNumGeneratorRepo.getNextSequenceNumber(sequenceType)
    }
    suspend fun getSettlementNumber(): String {
        val prefix = SequenceSuffix.SETTLEMENT.prefix
        val financialYear = getFormattedYear()
        val number = paymentNumGeneratorRepo.getNextSequenceNumber(prefix)
        return "${prefix}${financialYear}00000000$number"
    }

    private fun getFormattedYear(): String {
        val currYear = LocalDateTime.now().toString().split("-")[0].substring(2, 4)
        val lastYear = LocalDateTime.now().minusYears(1).toString().split("-")[0].substring(2, 4)
        return lastYear.plus(currYear)
    }
}
