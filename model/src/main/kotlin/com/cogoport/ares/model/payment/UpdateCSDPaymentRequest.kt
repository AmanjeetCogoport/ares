package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class UpdateCSDPaymentRequest(
    var paymentId: Long,
    var status: String,
    var updatedBy: UUID
)
