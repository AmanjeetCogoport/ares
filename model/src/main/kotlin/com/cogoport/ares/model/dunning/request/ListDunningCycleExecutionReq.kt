package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.common.Pagination
import com.cogoport.ares.model.dunning.enum.CycleType
import com.cogoport.ares.model.dunning.enum.DunningCycleStatus
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListDunningCycleExecutionReq(
    var query: String? = null,
    var cycleStatus: DunningCycleStatus? = null,
    var cycleType: CycleType? = null,
    var service: ServiceType? = null,
    var sortType: String? = "Desc",
    var sortBy: String? = "createdAt",
) : Pagination()
