package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class TradeAndServiceLevelOutstanding(
    @JsonProperty("name")
    var name: String?,
    @JsonProperty("key")
    var key: String?,
    @JsonProperty("openInvoiceAmount")
    var openInvoiceAmount: BigDecimal = BigDecimal.ZERO,
    @JsonProperty("currency")
    var currency: String?
)
