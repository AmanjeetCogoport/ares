package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SupplyAgentV2(
    @JsonProperty("id")
    var id: UUID?,
    @JsonProperty("name")
    var name: String?,
    @JsonProperty("email")
    var email: String?,
    @JsonProperty("mobileCountryCode")
    var mobileCountryCode: String?,
    @JsonProperty("mobileNumber")
    var mobileNumber: String?,
    @JsonProperty("stakeholder_type")
    var stakeholderType: String?,
)
