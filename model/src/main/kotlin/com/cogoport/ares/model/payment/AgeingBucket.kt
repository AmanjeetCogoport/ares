package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AgeingBucket(
    @JsonProperty("ageingDuration")
    var ageingDuration: String?,
    @JsonProperty("amount")
    var amount: BigDecimal?,
    @JsonProperty("count")
    var count: Int?,
    @JsonProperty("ageingDurationKey")
    var ageingDurationKey: String?
)
