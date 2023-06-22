package com.cogoport.ares.model.dunning.request

import java.util.UUID

data class UpdateOrganizationStakeholderRequest(
    var id: String,
    var creditControllerName: String?,
    var creditControllerId: UUID?,
    var organizationSegment: String?,
    var organizationStakeholderType: String?,
    var updatedBy: UUID
)
