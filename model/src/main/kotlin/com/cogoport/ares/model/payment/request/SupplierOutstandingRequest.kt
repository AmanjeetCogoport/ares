package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@Introspected
@MappedEntity
data class SupplierOutstandingRequest(
    var name: String? = null,
    var supplyAgentIds: List<UUID>? = null,
    var taxNo: String? = null,
    var countryIds: List<UUID>? = null,
    var cogoEntityId: UUID? = null,
    var ageingKey: List<String?> = listOf(),
    var companyType: String? = null,
    var category: String? = null,
    var sortBy: String? = null,
    var sortType: String? = null,
    var page: Int? = 1,
    var limit: Int? = 10
)
