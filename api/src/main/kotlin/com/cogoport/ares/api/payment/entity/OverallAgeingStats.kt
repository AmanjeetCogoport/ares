package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class OverallAgeingStats(
    var ageingDuration: String,
    var amount: BigDecimal,
    var dashboardCurrency: String? = "INR"
)
