package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class OrganizationCreditLimitDetailReq(
    @JsonProperty("org_id")
    var orgId: UUID,
    @JsonProperty("organization_trade_party_id")
    var organizationTradePartyId: UUID?
)
