package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class GetPartnerRequest(
    var id: UUID,
    @JsonProperty("service_objects_required")
    var serviceObjectRequired: Boolean = false
)