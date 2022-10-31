package com.cogoport.ares.api.common.models

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
data class ExchangeResponseForPeriod(
    @JsonProperty("from")
    var fromCurrencyType: String,
    @JsonProperty("to")
    var toCurrencyType: String,
    @JsonProperty("avg_exchange_rate")
    var exchangeRate: BigDecimal
)
