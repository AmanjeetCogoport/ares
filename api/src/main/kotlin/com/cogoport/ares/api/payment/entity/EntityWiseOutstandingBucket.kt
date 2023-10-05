package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class EntityWiseOutstandingBucket(
    var entityCode: String,
    var ledCurrency: String,
    var notDueLedAmount: BigDecimal,
    var thirtyLedAmount: BigDecimal,
    var fortyFiveLedAmount: BigDecimal,
    var sixtyLedAmount: BigDecimal,
    var ninetyLedAmount: BigDecimal,
    var oneEightyLedAmount: BigDecimal,
    var oneEightyPlusLedAmount: BigDecimal,
    var threeSixtyFiveLedAmount: BigDecimal,
    var threeSixtyFivePlusLedAmount: BigDecimal,
    var totalLedAmount: BigDecimal,
    var notDueCount: Int? = 0,
    var thirtyCount: Int? = 0,
    var fortyFiveCount: Int? = 0,
    var sixtyCount: Int? = 0,
    var ninetyCount: Int? = 0,
    var oneEightyCount: Int? = 0,
    var oneEightyPlusCount: Int? = 0,
    var threeSixtyFiveCount: Int? = 0,
    var threeSixtyFivePlusCount: Int? = 0,
    var totalCount: Int? = 0
)
