package com.cogoport.ares.model.payment

import java.math.BigDecimal

data class OverallStats(
    val totalOutstandingAmount: BigDecimal?,
    val openInvoicesCount: Int?,
    val organizationCount: Int?,
    val openInvoicesAmount: BigDecimal?,
    val openOnAccountPaymentAmount: BigDecimal?,
    var id: String?
)
