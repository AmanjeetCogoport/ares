package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class CreditControllerDetails(
    @JsonProperty("credit_controller_name")
    var creditControllerName: String? = null,
    @JsonProperty("credit_controller_email")
    var creditControllerEmail: String? = null,
    @JsonProperty("credit_controller_mobile_code")
    var creditControllerMobileCode: String? = null,
    @JsonProperty("credit_controller_mobile_number")
    var creditControllerMobileNumber: String? = null
)
