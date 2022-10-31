package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class AgeingBucketZone(
    val ageingDuration: String,
    var amount: BigDecimal,
    val zone: String,
    var dashboardCurrency: String? = "INR"
)
