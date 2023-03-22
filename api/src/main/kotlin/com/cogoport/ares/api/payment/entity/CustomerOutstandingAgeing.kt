package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class CustomerOutstandingAgeing(
    val organizationId: String?,
    val organizationName: String?,
    val entityCode: Int,
    val currency: String,
    val notDueAmount: BigDecimal?,
    val thirtyAmount: BigDecimal?,
    val sixtyAmount: BigDecimal?,
    val ninetyAmount: BigDecimal?,
    val oneEightyAmount: BigDecimal?,
    val threeSixtyFiveAmount: BigDecimal?,
    val todayAmount: BigDecimal?,
    val todayCount: Int? = 0,
    val threeSixtyFivePlusAmount: BigDecimal?,
    val totalOutstanding: BigDecimal?,
    val totalCreditAmount: BigDecimal?,
    val totalDebitAmount: BigDecimal?,
    val notDueCount: Int,
    val thirtyCount: Int,
    val sixtyCount: Int,
    val ninetyCount: Int,
    val oneEightyCount: Int,
    val threeSixtyFiveCount: Int,
    val threeSixtyFivePlusCount: Int,
    val creditNoteCount: Int,
    val debitNoteCount: Int
)
