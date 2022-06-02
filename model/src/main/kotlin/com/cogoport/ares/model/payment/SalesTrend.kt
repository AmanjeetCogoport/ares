package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class SalesTrend(
    @JsonProperty("month")
    val month: String,
    @JsonProperty("salesOnCredit")
    val salesOnCredit: Double
)
