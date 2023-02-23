package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.PaymentDocumentStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class PaymentDocumentStatusForPayments(
    var paymentDocumentStatus: PaymentDocumentStatus,
    var paymentIds: List<Long>
)
