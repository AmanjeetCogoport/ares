package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SummaryResponse(
    val amount: BigDecimal = 0.toBigDecimal(),
    val onAccountAmount: BigDecimal = 0.toBigDecimal()
)
