package com.cogoport.ares.api.migration.entity

import java.math.BigDecimal

data class JvResponse(
    val jvId: Long,
    val accountUtilizationId: Long,
    val amountLedger: BigDecimal,
    val payLedger: BigDecimal,
    val ledgerCurrency: String
)
