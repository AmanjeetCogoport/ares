package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class AgeingBucketZone(
    val ageingDuration: String,
    var amount: BigDecimal,
    val zone: String,
//    val serviceType: String?,
    var currencyType: String?,
    val invoiceCurrency: String?
)
