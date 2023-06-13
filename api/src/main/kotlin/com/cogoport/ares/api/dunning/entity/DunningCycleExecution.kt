package com.cogoport.ares.api.dunning.entity

import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.ares.model.dunning.enum.ScheduleType
import com.cogoport.ares.model.dunning.enum.TriggerType
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID
import javax.persistence.GeneratedValue

@MappedEntity(value = "dunning_cycle_executions")
data class DunningCycleExecution(
    @field:Id @GeneratedValue
    var id: Long?,
    var dunningCycleId: Long,
    var templateId: UUID,
    var status: CycleExecutionStatus,
    var filters: DunningCycleFilters,
    var scheduleRule: DunningScheduleRule,
    var scheduleType: ScheduleType,
    var scheduleAt: Timestamp,
    var triggerType: TriggerType,
    var deletedAt: Timestamp?,
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var createdBy: UUID,
    var updatedBy: UUID
)
