package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class AgeingBucket(
    @JsonProperty("ageingDuration")
    val ageingDuration: String = "",
    @JsonProperty("amount")
    val amount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("zone")
    val zone: String? = ""
)
