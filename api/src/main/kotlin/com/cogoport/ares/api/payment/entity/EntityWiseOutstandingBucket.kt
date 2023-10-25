package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class EntityWiseOutstandingBucket(
    var entityCode: String? = "301",
    var ledCurrency: String? = "INR",
    var notDueLedAmount: BigDecimal? = BigDecimal(0),
    var thirtyLedAmount: BigDecimal? = BigDecimal(0),
    var fortyFiveLedAmount: BigDecimal? = BigDecimal(0),
    var sixtyLedAmount: BigDecimal? = BigDecimal(0),
    var ninetyLedAmount: BigDecimal? = BigDecimal(0),
    var oneEightyLedAmount: BigDecimal? = BigDecimal(0),
    var oneEightyPlusLedAmount: BigDecimal? = BigDecimal(0),
    var threeSixtyFiveLedAmount: BigDecimal? = BigDecimal(0),
    var threeSixtyFivePlusLedAmount: BigDecimal? = BigDecimal(0),
    var totalLedAmount: BigDecimal? = BigDecimal(0),
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
