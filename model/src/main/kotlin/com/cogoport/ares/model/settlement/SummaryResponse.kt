package com.cogoport.ares.model.settlement

import java.math.BigDecimal

data class SummaryResponse(
    val amount: BigDecimal = 0.toBigDecimal()
)
