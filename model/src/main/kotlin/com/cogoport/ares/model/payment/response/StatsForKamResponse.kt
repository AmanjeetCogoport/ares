package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class StatsForKamResponse(
    val totalProformaAmount: BigDecimal?,
    val proformaInvoicesCount: Int?,
    val customersCountProforma: Int?,
    val totalDueAmount: BigDecimal?,
    val dueInvoicesCount: Int?,
    val customersCountDue: Int?,
    val totalOverdueAmount: BigDecimal?,
    val overdueInvoicesCount: Int?,
    val customersCountOverdue: Int?,
    val totalAmountReceivables: BigDecimal?,
    val receivablesInvoicesCount: Int?,
    val customersCountReceivables: Int?,
    val thirtyAmount: BigDecimal?,
    val sixtyAmount: BigDecimal?,
    val ninetyAmount: BigDecimal?,
    val ninetyPlusAmount: BigDecimal?,
    val thirtyCount: Int?,
    val sixtyCount: Int?,
    val ninetyCount: Int?,
    val ninetyPlusCount: Int?
)
