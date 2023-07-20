package com.cogoport.ares.api.dunning.model.request

import com.cogoport.ares.model.common.Pagination
import com.cogoport.ares.model.dunning.enum.DunningCycleStatus
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListDunningCycleReq(
    var query: String? = null,
    var cycleStatus: DunningCycleStatus? = null,
    var sortType: String? = "DESC",
    var dunningCycleType: String? = null,
    var sortBy: String? = "createdAt",
) : Pagination()
