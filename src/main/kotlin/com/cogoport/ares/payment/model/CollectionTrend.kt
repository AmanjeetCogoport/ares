package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class CollectionTrend(
    @JsonProperty("totalReceivableAmount")
    var totalReceivableAmount: Float,
    @JsonProperty("totalCollectedAmount")
    var totalCollectedAmount: Float,
    @JsonProperty("collectionTrend")
    var collectionTrend: List<CollectionTrendResponse>,
    @JsonProperty("docKey")
    var docKey: String
)
