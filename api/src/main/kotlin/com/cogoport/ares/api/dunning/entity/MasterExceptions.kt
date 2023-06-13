package com.cogoport.ares.api.dunning.entity

import com.cogoport.ares.api.dunning.model.OrgSegmentation
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@MappedEntity(value = "dunning_master_exceptions")
data class MasterExceptions(
    @field:Id @GeneratedValue var id: Long?,
    val tradePartyDetailId: UUID,
    val tradePartyName: String,
    val organizationId: UUID? = null,
    val registrationNumber: String,
    val orgSegment: OrgSegmentation? = null,
    val creditDays: Long? = null,
    val creditAmount: Long? = null,
    val isActive: Boolean?,
    val deletedAt: Timestamp? = null,
    @DateCreated
    val createdAt: Timestamp? = Timestamp.from(Instant.now()),
    @DateCreated
    val updatedAt: Timestamp? = Timestamp.from(Instant.now()),
    val createdBy: UUID,
    val updatedBy: UUID
)
