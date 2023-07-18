package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.dunning.enum.DunningCategory
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class CreateDunningCycleRequest(
    var name: String,
    var cycleType: String,
    var triggerType: String,
    var frequency: String,
    var severityLevel: String,
    var filters: DunningCycleFilters,
    var scheduleRule: DunningScheduleRule,
    var templateId: UUID,
    var category: String? = DunningCategory.CYCLE.toString(),
    var isActive: Boolean? = true,
    var createdBy: UUID,
    var exceptionTradePartyDetailIds: List<UUID>?
)
