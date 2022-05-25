package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class CollectionResponse(
    @JsonProperty("totalReceivableAmount")
    var totalReceivableAmount: Float?,
    @JsonProperty("totalCollectedAmount")
    var totalCollectedAmount: Float?,
    @JsonProperty("trend")
    var trend: MutableList<CollectionTrendResponse>,
    @JsonProperty("docKey")
    var id: String
)

