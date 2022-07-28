package com.cogoport.ares.model.settlement.event

import java.math.BigDecimal

data class InvoiceBalance(
    val invoiceId: Long,
    val balanceAmount: BigDecimal
)
