package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class DailySalesOutstanding(
    @JsonProperty("averageDsoForTheMonth")
    val averageDsoForTheMonth: Double,
    @JsonProperty("averageDsoLast3Months")
    val averageDsoLast3Months: Double,
    @JsonProperty("dsoResponse")
    val dsoResponse: List<DsoResponse>
)
