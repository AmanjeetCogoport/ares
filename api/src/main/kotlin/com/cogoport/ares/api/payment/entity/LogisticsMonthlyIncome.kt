package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class LogisticsMonthlyIncome(
    var totalIncomeJan: BigDecimal?,
    var totalIncomeFeb: BigDecimal?,
    var totalIncomeMarch: BigDecimal?,
    var totalIncomeApril: BigDecimal?,
    var totalIncomeMay: BigDecimal?,
    var totalIncomeJune: BigDecimal?,
    var totalIncomeJuly: BigDecimal?,
    var totalIncomeAugust: BigDecimal?,
    var totalIncomeSeptember: BigDecimal?,
    var totalIncomeOctober: BigDecimal?,
    var totalIncomeNovember: BigDecimal?,
    var totalIncomeDecember: BigDecimal?,
)
