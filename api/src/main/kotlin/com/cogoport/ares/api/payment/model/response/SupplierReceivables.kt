package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierReceivables(
        var totalReceivables: AmountAndCount,
        var unpaidReceivables: AmountAndCount,
        var partialPaidReceivables: AmountAndCount
)
