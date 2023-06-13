package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class GetOrganizationTradePartyDetailResponse(
    @JsonProperty("organization_trade_party_detail_id")
    var organizationTradePartyDetailId: UUID,
    @JsonProperty("registration_number")
    var registrationNumber: String
)
