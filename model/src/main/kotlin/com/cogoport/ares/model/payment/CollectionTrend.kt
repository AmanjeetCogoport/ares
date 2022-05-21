package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

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
