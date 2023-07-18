package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierStatistics(
    val currency: String,
    var invoicesDue: AmountAndCount,
    var onAccountPayment: AmountAndCount,
    var disputes: AmountAndCount? = null,
    var isComingSoonEnabled: Boolean
)
