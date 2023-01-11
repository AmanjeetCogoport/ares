package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class SupplierOutstandingRequest(
    var q: String? = null,
    var supplyAgentId: List<UUID>? = null,
    var countryId: List<UUID>? = null,
    var ageingKey: List<String?> = listOf(),
    var companyType: String? = null,
    var category: String? = null,
    var flag: String? = "overall",
    var sortBy: String? = null,
    var sortType: String? = "Desc",
    var page: Int? = 1,
    var limit: Int? = 10,
    var performedBy: UUID? = null
)
