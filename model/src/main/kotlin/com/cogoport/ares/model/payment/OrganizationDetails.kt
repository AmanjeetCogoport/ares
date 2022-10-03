package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class OrganizationDetails(
    @JsonProperty("serial_id")
    var orgSerialId: Long?
)
