package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@Introspected
@MappedEntity
data class LedgerDetails(
    val taggedOrganizationId: UUID,
    val transactionDate: Date,
    val ledgerCurrency: String,
    val serviceType: String,
    val documentValue: String,
    val documentNo: Long,
    val type: String,
    val debit: BigDecimal,
    val credit: BigDecimal
)
