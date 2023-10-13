package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity
data class OrgIdAndEntityCode(
    @JsonProperty("organization_id")
    val organizationId: UUID,
    @JsonProperty("entity_code")
    val entityCode: Int,
)
