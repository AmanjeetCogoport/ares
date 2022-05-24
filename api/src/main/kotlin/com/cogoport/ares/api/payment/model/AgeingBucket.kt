package com.cogoport.ares.api.payment.model

import java.math.BigDecimal

data class AgeingBucket(
    val ageingDuration: String,
    val amount: BigDecimal,
    val ageingDurationKey: String
) {
    @field:javax.persistence.Transient
    val currency: String = "INR"
}
