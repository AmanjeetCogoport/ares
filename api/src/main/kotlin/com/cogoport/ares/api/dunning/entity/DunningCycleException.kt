package com.cogoport.ares.api.dunning.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID
import javax.persistence.GeneratedValue

@MappedEntity(value = "dunning_cycle_exceptions")
data class DunningCycleException(
    @field:Id @GeneratedValue
    var id: Long?,
    var cycleId: Long,
    var tradePartyDetailId: UUID,
    var organizationId: UUID,
    var registrationNumber: String,
    var deletedAt: Timestamp?,
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var createdBy: UUID? = null,
    var updatedBy: UUID? = null
)
