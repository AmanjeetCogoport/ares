package com.cogoport.ares.model.dunning.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@MappedEntity
@Introspected
data class DunningCycleResponse(
    var id: String?,
    var name: String,
    var cycleType: String,
    var createdAt: Timestamp?,
    var updatedAt: Timestamp?,
)
