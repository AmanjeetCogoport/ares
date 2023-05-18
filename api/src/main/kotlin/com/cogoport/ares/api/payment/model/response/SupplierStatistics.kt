package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierStatistics(
        var invoicesDue: Statistics,
        var onAccountPayment: Statistics,
        var disputes: Statistics? = null
)
