package com.cogoport.ares.model.payment

import java.math.BigDecimal

data class AgeingBucketZone(
    val ageingDuration: String?,
    val amount: BigDecimal?,
    val zone: String?
)
