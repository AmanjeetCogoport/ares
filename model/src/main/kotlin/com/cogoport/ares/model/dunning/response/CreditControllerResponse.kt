package com.cogoport.ares.model.dunning.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@Introspected
@MappedEntity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CreditControllerResponse(
    @JsonProperty("organization_stakeholder_id")
    var organizationStakeholderId: UUID,
    @JsonProperty("organization_stakeholder_name")
    var organizationStakeholderName: String
)
