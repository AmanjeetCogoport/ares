package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class EntityWiseOutstandingBucket(
    val entityCode: Int,
    val ledCurrency: String,
    val notDueLedAmount: BigDecimal,
    val thirtyLedAmount: BigDecimal,
    val fortyFiveLedAmount: BigDecimal,
    val sixtyLedAmount: BigDecimal,
    val ninetyLedAmount: BigDecimal,
    val oneEightyLedAmount: BigDecimal,
    val oneEightyPlusLedAmount: BigDecimal,
    val threeSixtyFiveLedAmount: BigDecimal,
    val threeSixtyFivePlusLedAmount: BigDecimal,
    val totalLedAmount: BigDecimal,
    val notDueCount: Int,
    val thirtyCount: Int,
    val fortyFiveCount: Int,
    val sixtyCount: Int,
    val ninetyCount: Int,
    val oneEightyCount: Int,
    val oneEightyPlusCount: Int,
    val threeSixtyFiveCount: Int,
    val threeSixtyFivePlusCount: Int,
    val totalCount: Int
)
