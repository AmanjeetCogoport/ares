package com.cogoport.ares.api.dunning.entity

import com.cogoport.ares.model.dunning.enum.CycleType
import com.cogoport.ares.model.dunning.enum.DunningCatagory
import com.cogoport.ares.model.dunning.enum.ScheduleType
import com.cogoport.ares.model.dunning.enum.TriggerType
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID
import javax.persistence.GeneratedValue

@MappedEntity(value = "dunning_cycles")
data class DunningCycle(
    @field:Id @GeneratedValue
    var id: Long?,
    var name: String,
    var cycleType: CycleType,
    var triggerType: TriggerType,
    var scheduleType: ScheduleType,
    var severityLevel: Int,
    var filters: DunningCycleFilters, // TODO("Need to discuss").
    var scheduleRule: DunningScheduleRule, // TODO("Need to discuss").
    var templateId: UUID,
    var category: DunningCatagory,
    var isActive: Boolean?,
    var deletedAt: Timestamp?,
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var createdBy: UUID? = null,
    var updatedBy: UUID? = null
)
