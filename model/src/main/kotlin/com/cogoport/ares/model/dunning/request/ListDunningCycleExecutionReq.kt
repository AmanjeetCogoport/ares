package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.common.Pagination
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListDunningCycleExecutionReq(
    val dunningCycleId: String
) : Pagination()
