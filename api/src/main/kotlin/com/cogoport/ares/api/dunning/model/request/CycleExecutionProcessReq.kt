package com.cogoport.ares.api.dunning.model.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class CycleExecutionProcessReq(
    var scheduleId: String
)
