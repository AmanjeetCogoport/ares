package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class UpdateOrganizationDetailAresSideRequest(
    var billId: Long,
    var organizationId: UUID,
    var organizationTradePartyDetailId: UUID,
    var organizationTradePartiesId: UUID,
    var organizationTradePartySerialId: Long,
    var organizationTradePartyName: String,
    var updatedBy: UUID
)
