package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class OverallStats(
    var totalOutstandingAmount: BigDecimal?,
    val openInvoicesCount: Int?,
    val organizationCount: Int?,
    var openInvoicesAmount: BigDecimal?,
    var openOnAccountPaymentAmount: BigDecimal?,
    var id: String?,
    var dashboardCurrency: String
)
