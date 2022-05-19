package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class Dso(
    @JsonProperty("month")
    val month: String,
    @JsonProperty("dsoForTheMonth")
    val dsoForTheMonth: Double
)
