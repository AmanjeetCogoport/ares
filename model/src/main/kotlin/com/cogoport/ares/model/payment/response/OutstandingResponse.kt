package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.ServiceType
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
data class OutstandingResponse(
    @JsonProperty("duration")
    var duration: String?,
    @JsonProperty("amount")
    var amount: BigDecimal,
    @JsonProperty("serviceType")
    var serviceType: ServiceType? = ServiceType.NA,
    @JsonProperty("currencyType")
    var currencyType: String? = "INR"
)
