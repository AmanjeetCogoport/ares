package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SyncOrgStakeholderRequest(
    @JsonProperty("organization_stakeholder_name")
    var organizationStakeholderName: String?,
    @JsonProperty("organization_stakeholder_id")
    var organizationStakeholderId: UUID?,
    @JsonProperty("organization_stakeholder_type")
    var organizationStakeholderType: String,
    @JsonProperty("organization_id")
    var organizationId: UUID,
    @JsonProperty("organization_segment")
    var organizationSegment: String?,
    @JsonProperty("status")
    var status: String?,
    @JsonProperty("updated_by")
    var updatedBy: UUID?
)
