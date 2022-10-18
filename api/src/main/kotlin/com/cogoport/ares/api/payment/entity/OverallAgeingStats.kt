package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
@MappedEntity
data class OverallAgeingStats(
    var ageingDuration: String,
    var amount: BigDecimal,
    var serviceType: String?,
    var currencyType: String,
)
