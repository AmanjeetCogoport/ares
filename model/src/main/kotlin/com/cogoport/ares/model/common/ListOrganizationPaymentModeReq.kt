package com.cogoport.ares.model.common

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class ListOrganizationPaymentModeReq(
    var tradePartyDetailId: UUID,
    var tradePartyType: String,
    var status: String
)
