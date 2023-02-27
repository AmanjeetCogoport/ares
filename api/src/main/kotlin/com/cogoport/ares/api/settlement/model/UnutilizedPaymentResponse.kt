package com.cogoport.ares.api.settlement.model

import java.math.BigDecimal

data class UnutilizedPaymentResponse(
    val settlementId: Long?,
    var unUtilizedAmount: BigDecimal?
)
