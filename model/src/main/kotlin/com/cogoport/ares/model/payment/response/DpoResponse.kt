package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DpoResponse(
    @JsonProperty("month")
    val month: String,
    @JsonProperty("dpoForTheMonth")
    var dpoForTheMonth: BigDecimal,
    @JsonProperty("dashboardCurrency")
    var dashboardCurrency: String
)
