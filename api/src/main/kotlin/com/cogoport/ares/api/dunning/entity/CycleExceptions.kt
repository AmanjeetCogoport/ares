package com.cogoport.ares.api.dunning.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@MappedEntity(value = "dunning_cycle_exceptions")
data class CycleExceptions(
    @field:Id @GeneratedValue var id: Long?,
    val cycleId: Long,
    val tradePartyDetailId: UUID,
    val organizationId: UUID? = null,
    val registrationNumber: String,
    val deletedAt: Timestamp? = null,
    @DateCreated val createdAt: Timestamp? = Timestamp.from(Instant.now()),
    @DateCreated val updatedAt: Timestamp? = Timestamp.from(Instant.now()),
    val createdBy: UUID,
    val updatedBy: UUID
    )
