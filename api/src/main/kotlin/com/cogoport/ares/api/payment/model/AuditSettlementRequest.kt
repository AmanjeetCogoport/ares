package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.settlement.entity.Settlement

data class AuditSettlementRequest(
    var settlement: Settlement,
    var actionName: String,
    var performedById: String?,
    var performedByType: String?
)
