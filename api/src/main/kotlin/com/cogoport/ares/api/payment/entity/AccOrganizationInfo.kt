package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity
data class AccOrganizationInfo(
    val organizationId: UUID,
    val zoneCode: String,
    val orgSerialId: Long,
    val organizationName: String
)
