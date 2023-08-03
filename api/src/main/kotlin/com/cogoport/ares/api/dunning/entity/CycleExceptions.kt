package com.cogoport.ares.api.dunning.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity(value = "dunning_cycle_exceptions")
data class CycleExceptions(
    @field:Id @GeneratedValue var id: Long?,
    val dunningCycleId: Long,
    val tradePartyDetailId: UUID,
    val registrationNumber: String,
    val deletedAt: Timestamp? = null,
    @DateCreated
    var createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @DateUpdated
    var updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    val createdBy: UUID,
    val updatedBy: UUID
)
