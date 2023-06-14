package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.dunning.enum.DunningCatagory
import com.cogoport.ares.model.dunning.enum.DunningCycleType
import com.cogoport.ares.model.dunning.enum.ScheduleType
import com.cogoport.ares.model.dunning.enum.TriggerType
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp
import java.util.UUID

@Introspected
data class CreateDunningCycleRequest(
    var name: String,
    var dunningCycleType: DunningCycleType,
    var triggerType: TriggerType,
    var scheduleType: ScheduleType,
    var severityLevel: Int,
    var filters: DunningCycleFilters,
    var scheduleRule: DunningScheduleRule,
    var templateId: UUID,
    var category: DunningCatagory? = DunningCatagory.CYCLE,
    var isActive: Boolean?,
    var deletedAt: Timestamp?,
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var createdBy: UUID,
    var exceptionTradePartyDetailIds: List<UUID>
)
