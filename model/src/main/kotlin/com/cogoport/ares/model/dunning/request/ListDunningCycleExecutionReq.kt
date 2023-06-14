package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.common.Pagination
import com.cogoport.ares.model.dunning.enum.DunningCycleStatus
import com.cogoport.ares.model.dunning.enum.DunningCycleType
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListDunningCycleExecutionReq(
    var query: String? = null,
    var cycleStatus: DunningCycleStatus? = null,
    var dunningCycleType: DunningCycleType? = null,
    var service: ServiceType? = null,
    var sortType: String? = "DESC",
    var sortBy: String? = "createdAt",
) : Pagination()
