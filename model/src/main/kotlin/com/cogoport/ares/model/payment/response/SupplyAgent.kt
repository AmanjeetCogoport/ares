package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@ReflectiveAccess
data class SupplyAgent(
    @JsonProperty("id")
    var id: UUID?,
    @JsonProperty("name")
    var name: String?,
    @JsonProperty("email")
    var email: String?,
    @JsonProperty("mobileCountryCode")
    var mobileCountryCode: String?,
    @JsonProperty("mobileNumber")
    var mobileNumber: String?
)
