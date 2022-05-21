package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class Dso(
    val month: String,
    val dsoForTheMonth: Double
)
