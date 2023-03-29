package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.time.Month

@Introspected
data class BfIncomeExpenseResponse(
    var month: Month,
    var income: BigDecimal? = 0.toBigDecimal(),
    var expense: BigDecimal? = 0.toBigDecimal()
)
