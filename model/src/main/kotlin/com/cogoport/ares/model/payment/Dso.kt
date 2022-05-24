package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class Dso(
    @JsonProperty("month")
    val month: String,
    @JsonProperty("dsoForTheMonth")
    val dsoForTheMonth: Double
)
