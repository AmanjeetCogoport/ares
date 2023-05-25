package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class AmountAndCount(
        var amount: BigDecimal = BigDecimal.ZERO,
        var count: Int = 0
)
