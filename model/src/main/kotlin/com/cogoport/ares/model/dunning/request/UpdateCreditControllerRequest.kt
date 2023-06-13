package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.dunning.enum.OrganizationSegment
import java.util.UUID

data class UpdateCreditControllerRequest(
    var id: String,
    var creditControllerName: String?,
    var creditControllerId: UUID?,
    var organizationSegment: OrganizationSegment?,
    var createdBy: UUID?,
    var updatedBy: UUID
)
