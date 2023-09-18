package com.cogoport.ares.api.settlement.model

import com.cogoport.ares.model.settlement.ListOrganizationTradePartyDetailsResponse

data class JVValidationAndCollectedInformation(
    var errorParentId: MutableSet<String>,
    var errorFileUrl: String?,
    var tradePartyDetails: Map<String, ListOrganizationTradePartyDetailsResponse>
)
