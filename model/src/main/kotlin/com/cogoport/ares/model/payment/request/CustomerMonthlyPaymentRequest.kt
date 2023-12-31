package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class CustomerMonthlyPaymentRequest(
    var orgId: String,
    var year: String,
    var entityCode: Int
)
