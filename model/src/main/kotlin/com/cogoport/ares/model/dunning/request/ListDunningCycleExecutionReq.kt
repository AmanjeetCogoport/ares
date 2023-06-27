package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.common.Pagination
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListDunningCycleExecutionReq(
    var query: String? = null,
    var cycleStatus: List<String>? = null,
    var dunningCycleType: List<String>? = null,
    var serviceType: String? = null,
    var sortType: String? = "desc",
    var sortBy: String? = "updatedAt",
    var frequency: String? = null
) : Pagination()
