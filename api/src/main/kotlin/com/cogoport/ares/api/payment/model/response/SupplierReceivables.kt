package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierReceivables(
    val currency: String,
    var totalReceivables: AmountAndCount,
    var unpaidReceivables: AmountAndCount,
    var partialPaidReceivables: AmountAndCount,
    var isComingSoonEnabled: Boolean
)
