package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.InvoiceStatus
import java.math.BigDecimal
import java.util.Date

data class SettledInvoice(
    val id: Long?,
    val destinationId: Long,
    val destinationType: SettlementType,
    val currency: String?,
    val currentBalance: BigDecimal,
    val amount: BigDecimal?,
    val ledCurrency: String,
    val ledAmount: BigDecimal,
    var signFlag: Short,
    val settlementDate: Date,
    var status: String?
)
