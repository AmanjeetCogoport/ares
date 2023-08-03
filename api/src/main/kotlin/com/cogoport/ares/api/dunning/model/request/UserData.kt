package com.cogoport.ares.api.dunning.model.request

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    var workScopes: MutableList<String>? = mutableListOf(),
    @JsonProperty("isPartnerUser")
    var isPartnerUser: Boolean? = false
)
