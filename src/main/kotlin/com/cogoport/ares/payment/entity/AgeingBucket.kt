package com.cogoport.ares.payment.entity

import java.math.BigDecimal

data class AgeingBucket(
    val ageingDuration: String,
    val amount: BigDecimal,
    val zone: String
)
