package com.cogoport.ares.api.dunning.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@MappedEntity(value = "dunning_master_exceptions")
data class MasterExceptions (
    @field:Id @GeneratedValue var id: Long?,
    val tradePartyDetailId: UUID,
    val organizationId: UUID? = null,
    val segmentation:
    val registrationNumber: String,
    val deletedAt: Timestamp? = null,
    @DateCreated
    val createdAt: Timestamp? = Timestamp.from(Instant.now()),
    @DateCreated
    val updatedAt: Timestamp? = Timestamp.from(Instant.now()),
    val createdBy: UUID,
    val updatedBy: UUID
)