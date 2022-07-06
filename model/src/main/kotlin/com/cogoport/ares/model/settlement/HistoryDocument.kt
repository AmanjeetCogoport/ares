package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccountType
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

data class HistoryDocument(
    val id: Long?,
    val referenceNo: String,
    val sid: String?,
    val amount: BigDecimal,
    val utilizedAmount: BigDecimal,
    val organizationId: UUID,
    val accType: AccountType,
    val balance: BigDecimal,
    val transactionDate: Date,
    val lastEditedDate: Date,
)
