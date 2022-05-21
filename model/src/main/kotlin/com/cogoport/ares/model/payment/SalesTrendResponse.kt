package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class SalesTrendResponse(
    @JsonProperty("response")
    val response: List<SalesTrend>,
    @JsonProperty("docKey")
    val docKey: String
)
