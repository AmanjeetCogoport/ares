package com.cogoport.ares.model.dunning.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class UpdateDunningCycleExecutionStatusReq(
    var id: String,
    var updatedBy: UUID,
    var isDunningCycleActive: Boolean
)
