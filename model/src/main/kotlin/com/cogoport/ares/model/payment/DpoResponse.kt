package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class DpoResponse(
    @JsonProperty("month")
    val month: String,
    @JsonProperty("dpoForTheMonth")
    val dpoForTheMonth: Float
)
