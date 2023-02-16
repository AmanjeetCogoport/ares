package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class OnAccountPaymentRequest(
    var document: DocumentTdsRequest,
    var taggedDocuments: List<DocumentTdsRequest>,
    val createdBy: UUID?
)
