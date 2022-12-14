package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
@Introspected
data class StatsForTradePartyResponse(
    var organizationId: String,
    var overallStats: OverallStatsForTradeParty,
    var invoiceList: List<InvoiceDetailsResponse>?
)
