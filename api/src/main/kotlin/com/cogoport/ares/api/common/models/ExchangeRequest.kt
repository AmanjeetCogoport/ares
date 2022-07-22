package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class ExchangeRequest(
    @JsonProperty("from_curr")
    var from_curr: String,
    @JsonProperty("to_curr")
    var to_curr: String,
    @JsonProperty("exchange_date")
    var exchange_date: String,
)
