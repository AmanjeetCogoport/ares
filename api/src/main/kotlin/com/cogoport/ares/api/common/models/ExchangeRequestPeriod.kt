package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class ExchangeRequestPeriod(
    @JsonProperty("from_currency")
    var fromCurrency: String?,
    @JsonProperty("to_currency")
    var toCurrency: String?,
    @JsonProperty("start_date")
    var startDate: String?,
    @JsonProperty("end_date")
    var endDate: String?
)
