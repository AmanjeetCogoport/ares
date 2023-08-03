package com.cogoport.ares.model.dunning.response

import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.sql.Timestamp

@MappedEntity
data class DunningCycleExecutionResponse(
    var id: String,
    var dunningCycleId: String,
    var status: String,
    @MappedProperty(type = DataType.JSON)
    var filters: DunningCycleFilters,
    @MappedProperty(type = DataType.JSON)
    var scheduleRule: DunningScheduleRule,
    var frequency: String,
    var serviceId: String?,
    var scheduledAt: Timestamp,
    var triggerType: String,
    var createdAt: Timestamp,
)
