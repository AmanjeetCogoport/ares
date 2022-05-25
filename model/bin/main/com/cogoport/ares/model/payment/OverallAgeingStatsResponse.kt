package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class OverallAgeingStatsResponse (
    @JsonProperty("ageingDuration")
    var ageingDuration: String?,
    @JsonProperty("amount")
    var amount: BigDecimal?,
    @JsonProperty("count")
    var currency: String?,
    @JsonProperty("ageingDurationKey")
    var ageingDurationKey: String?
)