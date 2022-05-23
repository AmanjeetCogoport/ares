package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class DailySalesOutstanding(
    @JsonProperty("averageDsoForTheMonth")
    val averageDsoForTheMonth: Float,
    @JsonProperty("averageDsoLast3Months")
    val averageDsoLast3Months: Float,
    @JsonProperty("dso")
    val dsoResponse: List<DsoResponse>,
    @JsonProperty("docKey")
    val docKey: String
)
