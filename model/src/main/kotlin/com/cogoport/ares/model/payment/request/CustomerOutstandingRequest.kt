package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class CustomerOutstandingRequest(
    var q: String? = null,
    var salesAgentId: List<UUID>? = null,
    var kamId: List<UUID>? = null,
    var creditControllerId: List<UUID>? = null,
    var sageId: String? = null,
    var tradePartyDetailId: UUID? = null,
    var tradePartyDetailIds: List<UUID>? = null,
    var tradePartySerialId: String? = null,
    var organizationSerialId: String? = null,
    var countryId: List<UUID>? = null,
    var companyType: String? = null,
    var entityCode: String? = "101",
    var sortBy: String? = "totalOutstanding",
    var sortType: String? = "Desc",
    var page: Int? = 1,
    var limit: Int? = 10,
    var portfolioManagerId: List<UUID>? = null,
    var portfolioManagerRmId: List<UUID>? = null
)
