package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class GetOrganizationTradePartyDetailRequest(
    @JsonProperty("organization_trade_party_detail_ids")
    val organizationTradePartyDetailIds: List<UUID>
)
