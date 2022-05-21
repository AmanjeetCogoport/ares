package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class CollectionTrend(
    @JsonProperty("totalReceivableAmount")
    var totalReceivableAmount: Float,
    @JsonProperty("totalCollectedAmount")
    var totalCollectedAmount: Float,
    @JsonProperty("trend")
    var trend: List<CollectionTrendResponse>,
    @JsonProperty("docKey")
    var docKey: String
)
