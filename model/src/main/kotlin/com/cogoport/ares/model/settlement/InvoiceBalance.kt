package com.cogoport.ares.model.settlement

import java.math.BigDecimal
data class InvoiceBalance(
    val invoiceId: Long,
    val taxableAmount: BigDecimal

)
