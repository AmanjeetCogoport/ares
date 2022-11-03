package com.cogoport.ares.model.settlement.event

import java.math.BigDecimal
import java.util.UUID

data class InvoiceBalance(
    val invoiceId: Long,
    val balanceAmount: BigDecimal,
    val performedBy: UUID?,
    val performedByUserType: String?

)
