package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
@Introspected
data class TradePartyDetailRequest(
    var organizationTradePartyDetailId: String?,
    var organizationTradePartyDetailSerialId: Long?,
    var tradePartyType: String?
)
