package com.cogoport.ares.model.dunning.response

import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@MappedEntity
@Introspected
data class DunningCycleExecutionResponse(
    var name: String,
    var dunningCycleStatus: Boolean,
    var dunningCycleType: String,
    var id: String,
    var dunningCycleId: Long,
    var status: String,
    var filters: DunningCycleFilters,
    var scheduleRule: String,
    var scheduleType: String,
    var scheduleAt: Timestamp,
    var triggerType: String,
    var deletedAt: Timestamp?,
    var createdBy: UUID,
    var updatedBy: UUID
)
