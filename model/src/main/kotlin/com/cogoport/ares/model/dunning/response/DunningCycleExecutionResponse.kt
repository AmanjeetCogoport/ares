package com.cogoport.ares.model.dunning.response

import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.sql.Timestamp
import java.util.UUID

@MappedEntity
data class DunningCycleExecutionResponse(
    var name: String,
    var isDunningCycleActive: Boolean,
    var dunningCycleType: String,
    var id: String,
    var dunningCycleId: Long,
    var status: String,
    @MappedProperty(type = DataType.JSON)
    var filters: DunningCycleFilters,
    @MappedProperty(type = DataType.JSON)
    var scheduleRule: DunningScheduleRule,
    var scheduleType: String,
    var scheduledAt: Timestamp,
    var triggerType: String,
    var deletedAt: Timestamp?,
    var createdBy: UUID,
    var updatedBy: UUID,
    var createdAt: Timestamp,
    var updatedAt: Timestamp
)
