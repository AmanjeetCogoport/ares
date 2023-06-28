package com.cogoport.ares.api.dunning.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.GeneratedValue

@MappedEntity(value = "organization_stakeholders")
data class OrganizationStakeholder(
    @field:Id @GeneratedValue
    var id: Long?,
    var organizationStakeholderName: String,
    var organizationStakeholderId: UUID,
    var organizationId: UUID,
    var organizationStakeholderType: String,
    var organizationSegment: String,
    var isActive: Boolean? = true,
    @DateCreated
    var createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @DateUpdated
    var updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    var createdBy: UUID? = null,
    var updatedBy: UUID? = null
)
