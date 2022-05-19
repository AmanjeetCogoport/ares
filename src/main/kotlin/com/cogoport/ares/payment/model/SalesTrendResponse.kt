package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class SalesTrendResponse(
    @JsonProperty("response")
    val response: List<SalesTrend>,
    @JsonProperty("docKey")
    val docKey: String
)
