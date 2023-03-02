package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class ServiceLevelOutstanding(
    @JsonProperty("totalOutstanding")
    var totalOutstanding: BigDecimal?,
    @JsonProperty("openInvoiceAmount")
    var openInvoiceAmount: BigDecimal?,
    @JsonProperty("currency")
    var currency: String?,
    @JsonProperty("tradeType")
    var tradeType: List<TradeAndServiceLevelOutstanding>?
)
