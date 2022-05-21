package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class CollectionTrendResponse(
    @JsonProperty("duration")
    var duration: String,
    @JsonProperty("receivableAmount")
    var receivableAmount: Float,
    @JsonProperty("collectableAmount")
    var collectableAmount: Float,
)
