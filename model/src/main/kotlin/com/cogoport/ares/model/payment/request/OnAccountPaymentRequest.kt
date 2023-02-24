package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID

@Introspected
data class OnAccountPaymentRequest(
    var document: DocumentTdsRequest,
    var taggedDocuments: List<DocumentTdsRequest>,
    var settledAmount: BigDecimal,
    var settledTds: BigDecimal,
    val createdBy: UUID?
)
