package com.cogoport.ares.api.payment.model

import java.util.UUID

data class ReversePaymentRequest(
    val document: Long,
    val transferReferNo: String,
    val updatedBy: UUID?,
    val performedByType: String?
)
