package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
data class TradePartyOutstandingList(
    @JsonProperty("list")
    val list: List<TradePartyOutstanding?>? = null
)
