package com.cogoport.ares.api.dunning.entity

import com.cogoport.ares.model.dunning.enum.OrganizationSegment
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.GeneratedValue

@MappedEntity(value = "credit_controllers")
data class CreditController(
    @field:Id @GeneratedValue
    var id: Long?,
    var creditControllerName: String,
    var creditControllerId: UUID,
    var organizationId: UUID,
    var organizationSegment: OrganizationSegment,
    @DateCreated
    var createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @DateUpdated
    var updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    var createdBy: UUID? = null,
    var updatedBy: UUID? = null
)
