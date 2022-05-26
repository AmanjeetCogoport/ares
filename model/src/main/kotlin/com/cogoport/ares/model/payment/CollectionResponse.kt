package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class CollectionResponse(
    @JsonProperty("totalReceivableAmount")
    var totalReceivableAmount: Float?,
    @JsonProperty("totalCollectedAmount")
    var totalCollectedAmount: Float?,
    @JsonProperty("trend")
    var trend: MutableList<CollectionTrendResponse>,
    @JsonProperty("id")
    var id: String
)

