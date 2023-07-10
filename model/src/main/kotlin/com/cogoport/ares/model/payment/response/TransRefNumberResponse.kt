package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class TransRefNumberResponse(
    val paymentNum: Long,
    val transRefNumber: String?
)
