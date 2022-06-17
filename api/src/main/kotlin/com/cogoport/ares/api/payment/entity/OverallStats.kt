package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class OverallStats(
    val totalOutstandingAmount: BigDecimal?,
    val openInvoicesCount: Int?,
    val organizationCount: Int?,
    val openInvoicesAmount: BigDecimal?,
    val openOnAccountPaymentAmount: BigDecimal?,
    var id: String?
)
