package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@JsonInclude(JsonInclude.Include.NON_NULL)
@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ExchangeRateResponseByDate(
    @JsonProperty("from")
    var fromCurrency: String,
    @JsonProperty("to")
    var toCurrency: String,
    @JsonProperty("exchange_rate")
    var exchangeRate: BigDecimal,
    @JsonProperty("exchange_rate_date")
    var exchangeRateDate: String
)
