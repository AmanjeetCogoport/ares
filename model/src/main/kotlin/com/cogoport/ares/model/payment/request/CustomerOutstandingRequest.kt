package com.cogoport.ares.model.payment.request

import java.util.UUID

data class CustomerOutstandingRequest(
    var q: String? = null,
    var salesAgentId: List<UUID>? = null,
    var kamId: List<UUID>? = null,
    var creditController: List<UUID>? = null,
    var sageId: String? = null,
    var tradePartySerialId: String? = null,
    var organizationSerialId: String? = null,
    var countryId: List<UUID>? = null,
    var ageingKey: List<String?> = listOf(),
    var companyType: String? = null,
    var flag: String? = "overall",
    var sortBy: String? = null,
    var sortType: String? = "Desc",
    var page: Int? = 1,
    var limit: Int? = 10,
    var performedBy: UUID? = null
)
