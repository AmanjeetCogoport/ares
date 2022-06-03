package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.ReflectiveAccess

@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
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
