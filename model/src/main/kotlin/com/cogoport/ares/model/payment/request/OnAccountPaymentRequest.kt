package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class OnAccountPaymentRequest(
    var document: Long,
    var taggedDocuments: List<Long>,
    val createdBy: UUID?
)
