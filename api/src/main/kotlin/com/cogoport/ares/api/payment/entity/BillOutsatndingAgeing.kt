package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
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
    val totalCreditAmount: BigDecimal?,
    val notDueCount: Int,
    val thirtyCount: Int,
    val sixtyCount: Int,
    val ninetyCount: Int,
    val oneeightyCount: Int,
    val threesixfiveCount: Int,
    val threesixfiveplusCount: Int,
    val creditNoteCount: Int
)
