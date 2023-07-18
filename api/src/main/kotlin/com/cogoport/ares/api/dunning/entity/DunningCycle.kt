package com.cogoport.ares.api.dunning.entity

import com.cogoport.ares.model.dunning.enum.DunningCategory
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.GeneratedValue

@MappedEntity(value = "dunning_cycles")
@Introspected
data class DunningCycle(
    @field:Id @GeneratedValue
    var id: Long?,
    var name: String,
    var cycleType: String,
    var triggerType: String,
    var frequency: String,
    var severityLevel: Int,
    var entityCode: Int,
    @MappedProperty(type = DataType.JSON)
    var filters: DunningCycleFilters,
    @MappedProperty(type = DataType.JSON)
    var scheduleRule: DunningScheduleRule,
    var templateId: UUID?,
    var category: String? = DunningCategory.CYCLE.toString(),
    var isActive: Boolean?,
    var deletedAt: Timestamp?,
    @DateCreated
    var createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @DateUpdated
    var updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    var createdBy: UUID,
    var updatedBy: UUID
)
