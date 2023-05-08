package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.payment.repository.PaymentNumGeneratorRepo
import com.cogoport.ares.api.utils.Time
import com.cogoport.ares.model.payment.PaymentCode
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
    fun getFinancialYearSuffix(): String {
        val startYear = Time.getBeginningOfFinancialYear().toString()
        val endYear = Time.getEndOfFinancialYear().toString()

        val startYearSuffix = "${startYear[2]}${startYear[3]}"
        val endYearSuffix = "${endYear[2]}${endYear[3]}"

        return (startYearSuffix + endYearSuffix).replace("\\s".toRegex(), "")
    }

    suspend fun getPaymentNumAndValue(paymentCode: PaymentCode, paymentNum: Long?): Pair<String, Long> {
        val financialYearSuffix = getFinancialYearSuffix()
        val payNum = paymentNum ?: getPaymentNumber(paymentCode.name)
        return Pair(paymentCode.toString() + financialYearSuffix + payNum, payNum)
    }
}
