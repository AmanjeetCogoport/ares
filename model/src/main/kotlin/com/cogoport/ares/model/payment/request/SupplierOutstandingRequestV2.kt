package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class SupplierOutstandingRequestV2(
    var q: String? = null,
    var supplyAgentId: List<UUID>? = null,
    var creditControllerId: List<UUID>? = null,
    var orgIds: List<UUID>? = null,
    var companyType: String? = null,
    var entityCode: Int? = 301,
    var sortBy: String? = null,
    var sortType: String? = "Desc",
    var page: Int? = 1,
    var pageLimit: Int? = 10,
    var performedBy: UUID? = null
)
