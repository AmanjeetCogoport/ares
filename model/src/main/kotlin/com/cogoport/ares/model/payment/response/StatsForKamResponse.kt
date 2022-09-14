package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class StatsForKamResponse(
    val totalAmount: BigDecimal?,
    val invoicesCount: Int?,
    val customersCount: Int?
)
