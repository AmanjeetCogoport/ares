package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.util.Currency

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReceivableByAgeViaServiceType(
    @JsonProperty("serviceTypeName")
    var serviceTypeName: String?,
    @JsonProperty("ageingBucket")
    var ageingBucket: MutableList<AgeingBucketZone>,
)
