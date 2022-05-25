package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class OutstandingResponse(
    @JsonProperty("duration")
    var duration: String?,
    @JsonProperty("amount")
    var amount: BigDecimal
)
