package com.cogoport.ares.api.payment.model.requests

import com.cogoport.plutus.model.common.Pagination
import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected

@Introspected
data class BfProfitabilityReq(
    var q: String? = null,
    var jobStatus: String? = null,
    var sortType: String? = "Desc",
    @JsonFormat(pattern = "yyyy-MM-dd")
    var startDate: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var endDate: String? = null,
    var sortBy: String? = "createdAt",
    var entityCode: Int? = null,
    var serviceType: List<String>? = null
) : Pagination()
