package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class DefaultedBusinessPartnerRequest(
    var tradePartyDetailSerialId: Long,
    var tradePartyDetailId: UUID,
    var businessName: String
)
