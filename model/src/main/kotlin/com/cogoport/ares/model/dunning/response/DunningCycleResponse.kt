package com.cogoport.ares.model.dunning.response

import com.cogoport.ares.model.dunning.enum.DunningCatagory
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
data class DunningCycleResponse(
    var id: String?,
    var name: String,
    var dunningCycleType: DunningCycleType,
    var triggerType: TriggerType,
    var scheduleType: ScheduleType,
    var severityLevel: Int,
    var filters: DunningCycleFilters,
    var scheduleRule: DunningScheduleRule,
    var templateId: UUID?,
    var category: DunningCatagory?,
    var isActive: Boolean?,
    var deletedAt: Timestamp?,
    var createdAt: Timestamp?,
    var updatedAt: Timestamp?,
    var createdBy: UUID,
    var updatedBy: UUID
)
