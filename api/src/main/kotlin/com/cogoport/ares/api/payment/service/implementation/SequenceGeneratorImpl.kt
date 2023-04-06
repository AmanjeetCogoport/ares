package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.payment.repository.PaymentNumGeneratorRepo
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

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
        val now = LocalDate.now()
        var currentYear = now.year
        if (now.month < Month.APRIL) {
            currentYear -= 1
        }
        val startOfFinancialYear = LocalDate.of(currentYear, Month.APRIL, 1)
        val endOfFinancialYear = startOfFinancialYear.plusYears(1).minusDays(1)
        val formatter = DateTimeFormatter.ofPattern("yy")
        val startYear = startOfFinancialYear.format(formatter)
        val endYear = endOfFinancialYear.format(formatter)
        return startYear.plus(endYear)
    }
}
