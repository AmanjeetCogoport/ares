package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class CustomerOrgOutstanding(
    val organizationId: String?,
    val currency: String?,
    val openInvoicesCount: Int,
    val openInvoicesAmount: BigDecimal,
    val openInvoicesLedAmount: BigDecimal,
    val creditNoteCount: Int,
    val creditNoteAmount: BigDecimal,
    val creditNoteLedAmount: BigDecimal,
    val paymentsCount: Int,
    val paymentsAmount: BigDecimal,
    val paymentsLedAmount: BigDecimal,
    val outstandingAmount: BigDecimal,
    val outstandingLedAmount: BigDecimal
)
