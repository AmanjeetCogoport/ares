package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.dunning.enum.OrganizationSegment
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class CreditControllerRequest(
    var creditControllerName: String,
    var creditControllerId: UUID,
    var organizationId: UUID,
    var organizationSegment: OrganizationSegment,
    var createdBy: UUID,
    var updatedBy: UUID?
)
