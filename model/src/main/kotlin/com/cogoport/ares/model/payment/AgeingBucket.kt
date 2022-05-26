package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class AgeingBucket(
    @JsonProperty("ageingDuration")
    var ageingDuration: String?,
    @JsonProperty("amount")
    var amount: BigDecimal?,
    @JsonProperty("count")
    var count: Int?,
    @JsonProperty("ageingDurationKey")
    var ageingDurationKey: String?
)