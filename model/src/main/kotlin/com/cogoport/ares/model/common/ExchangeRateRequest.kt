package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class ExchangeRateRequest(
    @JsonProperty("from_currency")
    var fromCurrency: List<String>,
    @JsonProperty("to_currency")
    var toCurrency: List<String>,
    @field:JsonFormat(pattern = "yyyy-mm-dd")
    @JsonProperty("date_list")
    var transactionDates: List<String>,
)
