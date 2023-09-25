package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class CreditControllerDetails(
        @JsonProperty("name")
        var name: String? = null,
        @JsonProperty("email")
        var email: String? = null,
        @JsonProperty("mobileCountryCode")
        var mobileCountryCode: String? = null,
        @JsonProperty("mobileNumber")
        var mobileNumber: String? = null
)
