package com.cogoport.ares.api.payment.model.requests

import com.cogoport.plutus.model.common.Pagination
import io.micronaut.core.annotation.Introspected

@Introspected
data class BfProfitabilityReq(
    var q: String? = null,
    var jobStatus: String? = null,
    var sortType: String? = "Desc",
    var sortBy: String? = "createdAt",
    var entityCode: Int? = null
) : Pagination()
