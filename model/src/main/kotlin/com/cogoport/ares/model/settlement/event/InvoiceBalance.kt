package com.cogoport.ares.model.settlement.event

import com.cogoport.ares.model.PaymentStatus
import java.math.BigDecimal
import java.util.UUID

data class InvoiceBalance(
    val invoiceId: Long,
    val balanceAmount: BigDecimal,
    val performedBy: UUID?,
    val performedByUserType: String?,
    val paymentStatus: PaymentStatus
)
