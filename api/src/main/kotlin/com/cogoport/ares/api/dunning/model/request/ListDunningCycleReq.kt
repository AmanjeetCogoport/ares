package com.cogoport.ares.api.dunning.model.request

import com.cogoport.ares.model.common.Pagination
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListDunningCycleReq(
    var query: String? = null,
    var cycleStatus: String? = null,
    var sortType: String? = "Desc",
    var sortBy: String? = "createdAt",
) : Pagination()
