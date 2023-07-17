package com.cogoport.ares.api.dunning.model.request

import com.cogoport.ares.model.common.Pagination
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListExceptionReq(
    var segmentation: String? = null,
    var query: String? = null,
    var creditDays: Long? = null,
    var sortType: String? = "Desc",
    var sortBy: String? = "dueAmount",
    var entities: List<Long>?,
    var cycleId: String? = null
) : Pagination()
