package com.cogoport.ares.model.settlement

import java.math.BigDecimal

data class EditTdsRequest(
    val documentNo: Long,
    val settlementType: SettlementType,
    val newTds: BigDecimal,
    val newLedTds: BigDecimal
)
