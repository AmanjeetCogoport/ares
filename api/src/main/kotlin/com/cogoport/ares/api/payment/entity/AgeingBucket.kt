package com.cogoport.ares.api.payment.entity

import java.math.BigDecimal

data class AgeingBucket(
    val ageingDuration: String,
    val amount: BigDecimal,
    val zone: String
)
