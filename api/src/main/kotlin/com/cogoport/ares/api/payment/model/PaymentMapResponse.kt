package com.cogoport.ares.api.payment.model

import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class PaymentMapResponse(
    var id: Long,
    var paymentId: Long
)
