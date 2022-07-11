package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccountType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
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
