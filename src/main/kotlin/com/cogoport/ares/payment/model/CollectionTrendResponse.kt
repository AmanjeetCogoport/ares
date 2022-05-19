package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CollectionTrendResponse(
    @JsonProperty("duration")
    var duration: String,
    @JsonProperty("receiveableAmount")
    var receiveableAmount: Float,
    @JsonProperty("collectableAmount")
    var collectableAmount: Float,
)