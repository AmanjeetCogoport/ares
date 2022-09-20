package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class KamPaymentRequest(
    val docValue: List<String>
)
