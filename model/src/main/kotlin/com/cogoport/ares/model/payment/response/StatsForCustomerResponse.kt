package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@Introspected
@MappedEntity
data class StatsForCustomerResponse(
    val organizationId: UUID?,
    val totalProformaAmount: BigDecimal?,
    val proformaInvoicesCount: Int?,
    val totalDueAmount: BigDecimal?,
    val dueInvoicesCount: Int?,
    val totalOverdueAmount: BigDecimal?,
    val overdueInvoicesCount: Int?,
    val totalAmountReceivables: BigDecimal?,
    val receivablesInvoicesCount: Int?,
    val onAccountPayment: BigDecimal?,
    val thirtyAmount: BigDecimal?,
    val sixtyAmount: BigDecimal?,
    val ninetyAmount: BigDecimal?,
    val ninetyPlusAmount: BigDecimal?,
    val thirtyCount: Int?,
    val sixtyCount: Int?,
    val ninetyCount: Int?,
    val ninetyPlusCount: Int?
)
