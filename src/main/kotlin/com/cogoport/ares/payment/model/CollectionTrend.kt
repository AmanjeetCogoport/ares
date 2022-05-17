package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class CollectionTrend(
    @JsonProperty("totalAmount")
    var totalAmount: Map<String, BigDecimal>,
    @JsonProperty("month1")
    var month1: Map<String, BigDecimal>,
    @JsonProperty("month2")
    var month2: Map<String, BigDecimal>,
    @JsonProperty("month3")
    var month3: Map<String, BigDecimal>,
    @JsonProperty("docKey")
    var docKey: String
)
