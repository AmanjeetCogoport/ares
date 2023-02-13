package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class OnAccountPaymentRequest(
    val documentNo: String,
    val taggedDocuments: List<String>,
    val createdBy: UUID?,
    val paymentUploadIds: List<Long>
)
