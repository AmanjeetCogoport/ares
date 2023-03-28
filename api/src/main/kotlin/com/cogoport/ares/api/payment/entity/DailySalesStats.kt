package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class DailySalesStats(
    var duration: String?,
    var amount: BigDecimal,
    var dashboardCurrency: String? = "INR",
    var count: Long? = 0L
)
