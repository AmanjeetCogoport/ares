package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.common.Pagination
import io.micronaut.core.annotation.Introspected

@Introspected
data class OverallOutstandingAndOnAccountRequest(
    var entityCodes: List<Int>? = null,
    var query: String? = null,
    var serviceTypes: List<String>? = null,
    var sortBy: String? = null,
    var sortType: String? = null
) : Pagination()
