package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class TicketGenerationReq(
    @JsonProperty("Name")
    val name: String?,
    @JsonProperty("Email")
    val email: String?,
    @JsonProperty("MobileCountryCode")
    val mobileCountryCode: String?,
    @JsonProperty("MobileNumber")
    val mobileNumber: String?,
    @JsonProperty("Source")
    val source: String?,
    @JsonProperty("Type")
    val type: String?,
    @JsonProperty("Status")
    val status: String?,
    @JsonProperty("SystemUserId")
    val systemUserId: String?,
    @JsonProperty("OrganizationId")
    val organizationId: String?,
)
