package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class OverallAgeingStatsResponse(
    @JsonProperty("ageingDuration")
    var ageingDuration: String?,
    @JsonProperty("amount")
    var amount: BigDecimal?,
    @JsonProperty("currency")
    var currency: String?
)
