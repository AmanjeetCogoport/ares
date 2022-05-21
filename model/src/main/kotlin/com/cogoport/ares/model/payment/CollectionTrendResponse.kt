package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class CollectionTrendResponse(
    @JsonProperty("duration")
    var duration: String,
    @JsonProperty("receiveableAmount")
    var receiveableAmount: Float,
    @JsonProperty("collectableAmount")
    var collectableAmount: Float,
)
