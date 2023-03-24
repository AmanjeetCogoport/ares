package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.PaymentDocumentStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class SageStatusResponse(
    var sageNumValue: String?,
    var paymentDocumentStatus: PaymentDocumentStatus
)
