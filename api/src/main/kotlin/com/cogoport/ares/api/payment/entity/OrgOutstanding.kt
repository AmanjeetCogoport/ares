package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class OrgOutstanding(
    val organizationName: String?,
    val openInvoiceCount: Int?,
    val invoiceAmountInr: BigDecimal?,
    val invoiceAmountUsd: BigDecimal?,
    val onAccountPaymentCount: Int?,
    val onAccountPaymentInr: BigDecimal?,
    val onAccountPaymentUsd: BigDecimal?,
    val outstandingInr: BigDecimal?,
    val outstandingUsd: BigDecimal?
)
