package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
data class CollectionResponse(
    @JsonProperty("totalReceivableAmount")
    var totalReceivableAmount: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("totalCollectedAmount")
    var totalCollectedAmount: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("trend")
    var trend: List<CollectionTrendResponse>? = null,
    @JsonProperty("id")
    var id: String
)
