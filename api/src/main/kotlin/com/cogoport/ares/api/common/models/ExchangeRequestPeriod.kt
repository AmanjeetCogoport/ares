package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
class ExchangeRequestPeriod (
    @JsonProperty("from_currency")
    var from_currency: String?,
    @JsonProperty("to_currency")
    var to_currency: String?,
    @JsonProperty("start_date")
    var start_date: String?,
    @JsonProperty("end_date")
    var end_date: String?
)