package com.cogoport.ares.api.payment.model

import java.math.BigDecimal
import java.util.UUID

data class ReversePaymentRequest(
    val document: Long,
    val source: Long,
    val exchangeRate: BigDecimal,
    val updatedBy: UUID?,
    val performedByType: String?
)
