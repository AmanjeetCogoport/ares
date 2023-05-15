package com.cogoport.ares.model.settlement

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class PostPaymentToSage(
    var paymentId: Long,
    var performedBy: UUID
)
