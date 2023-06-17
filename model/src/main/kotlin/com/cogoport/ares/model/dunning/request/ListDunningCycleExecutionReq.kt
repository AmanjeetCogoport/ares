package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.common.Pagination
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListDunningCycleExecutionReq(
    var query: String? = null,
    var cycleStatus: List<String>? = null,
    var dunningCycleType: List<String>? = null,
    var service: List<String>? = null,
    var sortType: String? = "DESC",
    var sortBy: String? = "updated_at",
) : Pagination()
