package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class DailySalesOutstanding(
    @JsonProperty("averageDsoForTheMonth")
    val averageDsoForTheMonth: Float,
    @JsonProperty("averageDsoLast3Months")
    val averageDsoLast3Months: Float,
    @JsonProperty("dso")
    val dso: List<Dso>,
    @JsonProperty("docKey")
    val docKey: String
)
