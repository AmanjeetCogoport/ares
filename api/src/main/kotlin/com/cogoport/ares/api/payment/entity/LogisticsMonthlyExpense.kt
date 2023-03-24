package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class LogisticsMonthlyExpense(
    var totalExpenseJan: BigDecimal?,
    var totalExpenseFeb: BigDecimal?,
    var totalExpenseMarch: BigDecimal?,
    var totalExpenseApril: BigDecimal?,
    var totalExpenseMay: BigDecimal?,
    var totalExpenseJune: BigDecimal?,
    var totalExpenseJuly: BigDecimal?,
    var totalExpenseAugust: BigDecimal?,
    var totalExpenseSeptember: BigDecimal?,
    var totalExpenseOctober: BigDecimal?,
    var totalExpenseNovember: BigDecimal?,
    var totalExpenseDecember: BigDecimal?,
)
