package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class TradePartyStatsRequest(
    val docValues: List<String>,
    val orgId: String?,
    val pageIndex: Int,
    val pageSize: Int,
    val sortType: String?,
    val sortBy: String?
)
