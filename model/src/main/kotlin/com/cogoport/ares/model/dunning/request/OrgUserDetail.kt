package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class OrgUserDetail(
    @JsonProperty("mobile_number")
    val mobileNumber: String?,
    @JsonProperty("mobile_country_code")
    val mobileCountryCode: String?,
    @JsonProperty("email")
    val email: String?,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("work_scopes")
    val workScopes: MutableList<String>?,
)
