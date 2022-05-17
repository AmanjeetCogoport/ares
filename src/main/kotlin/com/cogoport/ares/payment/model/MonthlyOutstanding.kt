package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class MonthlyOutstanding(
    @JsonProperty("trend")
    var trend: Map<String, BigDecimal>,
    @JsonProperty("docKey")
    var docKey: String
)
