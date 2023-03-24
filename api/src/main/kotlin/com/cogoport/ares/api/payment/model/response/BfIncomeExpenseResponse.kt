package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.api.payment.entity.LogisticsMonthlyExpense
import com.cogoport.ares.api.payment.entity.LogisticsMonthlyIncome
import io.micronaut.core.annotation.Introspected

@Introspected
data class BfIncomeExpenseResponse(
    var logisticsMonthlyIncome: LogisticsMonthlyIncome,
    var logisticsMonthlyExpense: LogisticsMonthlyExpense
)
