package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class CollectionTrend(
    @JsonProperty("totalReceivableAmount")
    var totalReceivableAmount: BigDecimal?,
    @JsonProperty("totalCollectedAmount")
    var totalCollectedAmount: BigDecimal?,
    @JsonProperty("trend")
    var trend: MutableList<CollectionTrendResponse>,
    @JsonProperty("docKey")
    var docKey: String
)
