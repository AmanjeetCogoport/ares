package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class Outstanding(
    var duration: String?,
    var dashboardCurrency: String? = "INR",
    var openInvoiceAmount: BigDecimal? = BigDecimal.ZERO,
    var totalOutstandingAmount: BigDecimal? = BigDecimal.ZERO,
    var totalSales: BigDecimal? = BigDecimal.ZERO
)
