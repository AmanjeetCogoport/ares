package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.settlement.SettlementType
import java.util.UUID

data class DeleteSettlementRequest(
    val documentNo: String,
    val settlementType: SettlementType,
    val deletedBy: UUID,
    val deletedByUserType: String
)
