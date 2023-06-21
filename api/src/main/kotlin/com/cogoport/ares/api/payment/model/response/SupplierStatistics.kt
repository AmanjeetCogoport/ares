package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierStatistics(
    var invoicesDue: AmountAndCount,
    var onAccountPayment: AmountAndCount,
    var disputes: AmountAndCount? = null
)
