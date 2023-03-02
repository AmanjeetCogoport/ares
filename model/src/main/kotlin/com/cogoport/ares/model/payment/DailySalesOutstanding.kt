package com.cogoport.ares.model.payment

import com.cogoport.ares.model.payment.response.DsoResponse
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DailySalesOutstanding(
    @JsonProperty("dsoResponse")
    val dsoResponse: List<DsoResponse>
)
