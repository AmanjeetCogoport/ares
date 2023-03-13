package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID

@Introspected
data class OnAccountPaymentRequest(
    var document: DocumentRequest,
    var taggedDocuments: List<DocumentRequest>,
    var settledAmount: BigDecimal,
    val createdBy: UUID?
)
