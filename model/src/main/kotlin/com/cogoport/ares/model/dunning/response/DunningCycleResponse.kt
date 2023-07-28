package com.cogoport.ares.model.dunning.response

import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.sql.Timestamp

@MappedEntity
@Introspected
data class DunningCycleResponse(
    var id: String,
    var name: String,
    var triggerType: String,
    var frequency: String,
    @MappedProperty(type = DataType.JSON)
    var scheduleRule: DunningScheduleRule,
    @MappedProperty(type = DataType.JSON)
    var filters: DunningCycleFilters,
    var cycleType: String,
    var createdAt: Timestamp?,
    var updatedAt: Timestamp?,
    var isDunningCycleActive: Boolean
)
