package com.cogoport.ares.api.dunning.model.response

import io.micronaut.data.annotation.MappedEntity
import java.util.Date

@MappedEntity
data class ListDunningCycleResp(
    var id: String,
    var cycleName: String,
    var cycleType: String,
    var schedule_type: String,
    var createdAt: Date,
    var updatedAt: Date
)
