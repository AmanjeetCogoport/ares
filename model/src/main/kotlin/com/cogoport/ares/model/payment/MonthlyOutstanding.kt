package com.cogoport.ares.model.payment

import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
data class MonthlyOutstanding(
    @JsonProperty("list")
    var list: List<OutstandingResponse>? = null,
    @JsonProperty("id")
    var id: String? = null
)
