package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class OverallStats(
    var totalOutstandingAmount: BigDecimal?,
    var openInvoicesCount: Int?,
    var openInvoicesAmount: BigDecimal?,
    var openOnAccountPaymentAmount: BigDecimal?,
    var dashboardCurrency: String,
    var customersCount: Int? = 0
)
