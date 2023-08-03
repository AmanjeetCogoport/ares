package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class CustomerOutstandingAgeing(
    val organizationId: String?,
    val organizationName: String?,
    val entityCode: Int,
    val currency: String,
    val notDueLedAmount: BigDecimal,
    val thirtyLedAmount: BigDecimal,
    val fortyFiveLedAmount: BigDecimal,
    val sixtyLedAmount: BigDecimal,
    val ninetyLedAmount: BigDecimal,
    val oneEightyLedAmount: BigDecimal,
    val oneEightyPlusLedAmount: BigDecimal,
    val threeSixtyFiveLedAmount: BigDecimal,
    val threeSixtyFivePlusLedAmount: BigDecimal,
    val totalLedOutstanding: BigDecimal,
    val notDueCurrAmount: BigDecimal,
    val thirtyCurrAmount: BigDecimal,
    val fortyFiveCurrAmount: BigDecimal,
    val sixtyCurrAmount: BigDecimal,
    val ninetyCurrAmount: BigDecimal,
    val oneEightyCurrAmount: BigDecimal,
    val oneEightyPlusCurrAmount: BigDecimal,
    val threeSixtyFiveCurrAmount: BigDecimal,
    val threeSixtyFivePlusCurrAmount: BigDecimal,
    val totalCurrOutstanding: BigDecimal,
    val notDueCount: Int,
    val thirtyCount: Int,
    val fortyFiveCount: Int,
    val sixtyCount: Int,
    val ninetyCount: Int,
    val oneEightyCount: Int,
    val oneEightyPlusCount: Int,
    val threeSixtyFiveCount: Int,
    val threeSixtyFivePlusCount: Int
)
