package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class DsoResponse(
    @JsonProperty("month")
    val month: Int,
    @JsonProperty("dsoForTheMonth")
    val dsoForTheMonth: Float
)
