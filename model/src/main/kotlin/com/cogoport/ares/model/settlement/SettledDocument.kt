package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.KnockOffStatus
import com.cogoport.ares.model.payment.AccountType
import java.math.BigDecimal
import java.util.Date

data class SettledDocument(
    val id: Long?,
    val documentNo: String,
    val sid: String?,
    val amount: BigDecimal,
    val currentBalance: BigDecimal,
    val accType: AccountType,
    val transactionDate: Date,
    val settlementStatus: KnockOffStatus
)
