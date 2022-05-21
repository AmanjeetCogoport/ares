package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class SalesTrend(
    @JsonProperty("month")
    val month: String,
    @JsonProperty("salesOnCredit")
    val salesOnCredit: Double
)
