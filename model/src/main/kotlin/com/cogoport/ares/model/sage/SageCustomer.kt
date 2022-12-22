package com.cogoport.ares.model.sage

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class SageCustomer(
    @JsonProperty("BPCNUM_0")
    val sageOrganizationId: String
)
