package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.ReflectiveAccess

@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
data class CollectionResponse(
    @JsonProperty("totalReceivableAmount")
    var totalReceivableAmount: Float? = 0F,
    @JsonProperty("totalCollectedAmount")
    var totalCollectedAmount: Float? = 0F,
    @JsonProperty("trend")
    var trend: List<CollectionTrendResponse>? = null,
    @JsonProperty("id")
    var id: String
)
