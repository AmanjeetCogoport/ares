package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.InvoiceStats
import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierReceivablesAndAgeingBucket(
        var totalReceivables: InvoiceStats,
        var unpaidReceivables: InvoiceStats,
        var partialPaidReceivables: InvoiceStats,
        var ageingBucket: List<AgeingBucket>
)
