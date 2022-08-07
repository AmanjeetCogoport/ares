package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class GetOrgDetailsResponse(
    @JsonProperty("organization_serial_id")
    val organizationSerialId: String? = null,
    @JsonProperty("organization_id")
    val organizationId: String? = null,
    @JsonProperty("zone")
    val zone: String? = null
)
