package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class OrgOutstanding(
    val organizationId: String?,
    val organizationName: String?,
    val currency: String?,
    val openInvoicesCount: Int?,
    val openInvoicesAmount: BigDecimal?,
    val openInvoicesLedAmount: BigDecimal?,
    val paymentsCount: Int?,
    val paymentsAmount: BigDecimal?,
    val paymentsLedAmount: BigDecimal?,
    val outstandingAmount: BigDecimal?,
    val outstandingLedAmount: BigDecimal?,
    val zoneCode: String?
)
