package com.cogoport.ares.model.settlement.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class BulkPostSettlementRequest(
    var settlementIds: List<Long>,
    var performedBy: UUID
)
