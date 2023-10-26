package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class HookToAresRequest(
    @JsonProperty("organization_trade_party_detail_id")
    val organizationTradePartyDetailId: UUID
)
