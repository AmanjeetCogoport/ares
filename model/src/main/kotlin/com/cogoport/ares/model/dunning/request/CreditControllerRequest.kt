package com.cogoport.ares.model.dunning.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class CreditControllerRequest(
    var creditControllerName: String,
    var creditControllerId: UUID,
    var organizationId: UUID,
    var organizationSegment: String,
    var organizationStakeholderType: String,
    var createdBy: UUID
)
