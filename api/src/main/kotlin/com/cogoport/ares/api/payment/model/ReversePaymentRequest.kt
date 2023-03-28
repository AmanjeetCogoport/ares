package com.cogoport.ares.api.payment.model

import java.util.UUID

data class ReversePaymentRequest(
    val document: Long,
    val source: Long,
    val updatedBy: UUID?,
    val performedByType: String?
)
