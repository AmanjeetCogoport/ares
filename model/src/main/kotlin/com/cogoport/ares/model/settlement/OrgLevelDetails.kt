package com.cogoport.ares.model.settlement

import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity
data class OrgLevelDetails(
    val bpr: String?,
    val organizationId: UUID?,
    val orgSerialId: Long?,
    val businessName: String?
)
