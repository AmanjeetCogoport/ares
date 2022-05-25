package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class AgeingBucket(
    val ageingDuration: String,
    val amount: BigDecimal,
    val zone: String?
)
