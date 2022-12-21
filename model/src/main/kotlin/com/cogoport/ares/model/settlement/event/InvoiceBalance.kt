package com.cogoport.ares.model.settlement.event

import com.cogoport.ares.model.PaymentStatus
import com.cogoport.ares.model.settlement.SettlementType
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

data class InvoiceBalance(
    val invoiceId: Long,
    val balanceAmount: BigDecimal,
    val performedBy: UUID?,
    val performedByUserType: String?,
    val paymentStatus: PaymentStatus,
    var transRefNumber: String? = null,
    var settlementDate: Timestamp?,
    var sourceType: SettlementType?,
    var sourceId: Int?
)
