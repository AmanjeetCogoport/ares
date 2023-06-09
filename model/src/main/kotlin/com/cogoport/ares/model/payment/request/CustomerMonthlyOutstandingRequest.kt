package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class CustomerMonthlyOutstandingRequest(
        var orgId: String,
        var year: String
)
