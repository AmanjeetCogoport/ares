package com.cogoport.ares.api.dunning.model.request

import com.cogoport.ares.api.dunning.model.OrgSegmentation
import com.cogoport.kuber.model.expense.enums.SortType
import io.micronaut.core.annotation.Introspected

@Introspected
data class ListMasterExceptionReq(
    var segmentation: List<OrgSegmentation>? = null,
    var query: String? = null,
    var sortType: String? = "Desc",
    var sortBy: String? = "dueAmount"
)
