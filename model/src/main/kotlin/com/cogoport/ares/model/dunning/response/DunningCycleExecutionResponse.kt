package com.cogoport.ares.model.dunning.response

import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.ares.model.dunning.enum.DunningCycleType
import com.cogoport.ares.model.dunning.enum.ScheduleType
import com.cogoport.ares.model.dunning.enum.TriggerType
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@MappedEntity
@Introspected
data class DunningCycleExecutionResponse(
    var name: String,
    var dunningCycleStatus: Boolean,
    var dunningCycleType: DunningCycleType,
    var id: String,
    var dunningCycleId: Long,
    var status: CycleExecutionStatus,
    var filters: DunningCycleFilters,
    var scheduleRule: DunningScheduleRule,
    var scheduleType: ScheduleType,
    var scheduleAt: Timestamp,
    var triggerType: TriggerType,
    var deletedAt: Timestamp?,
    var createdBy: UUID,
    var updatedBy: UUID
)
