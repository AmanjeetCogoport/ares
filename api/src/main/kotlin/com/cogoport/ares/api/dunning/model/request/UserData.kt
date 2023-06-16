package com.cogoport.ares.api.dunning.model.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class UserData(
    @JsonProperty("name")
    var name: String? = null,
    @JsonProperty("mobile_verified")
    var mobileVerified: String? = null,
    @JsonProperty("email")
    var email: String? = null,
    @JsonProperty("mobile_number")
    var mobileNumber: String? = null,
    @JsonProperty("user_id")
    var userId: String? = null,
    @JsonProperty("mobile_country_code")
    var mobileCountryCode: String? = null,
    @JsonProperty("work_scopes")
    var workScopes: MutableList<String>? = mutableListOf()
)
