package com.cogoport.ares.api.payment.entity

import java.math.BigDecimal

data class BillOutsatndingAgeing(
    val organizationId: String?,
    val organizationName: String?,
    val notDueAmount: BigDecimal?,
    val thirtyAmount: BigDecimal?,
    val sixtyAmount: BigDecimal?,
    val ninetyAmount: BigDecimal?,
    val oneeightyAmount: BigDecimal?,
    val threesixfiveAmount: BigDecimal?,
    val threesixfiveplusAmount: BigDecimal?,
    val totalOutstanding: BigDecimal?,
    val notDueCount: Int,
    val thirtyCount: Int,
    val sixtyCount: Int,
    val ninetyCount: Int,
    val oneeightyCount: Int,
    val threesixfiveCount: Int,
    val threesixfiveplusCount: Int
)
