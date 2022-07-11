package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccountType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@MappedEntity
data class HistoryDocument(
    val id: Long?,
    val referenceNo: String,
    val sid: String?,
    val amount: BigDecimal,
    val currency: String,
    val utilizedAmount: BigDecimal,
    val organizationId: UUID,
    val accType: AccountType,
    val balance: BigDecimal,
    val transactionDate: Date,
    val lastEditedDate: Date,
)
