package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class AgeingBucket(
    val ageingDuration: String?,
    val amount: BigDecimal?,
    val count: Int?,
    val ageingDurationKey: String?
)
