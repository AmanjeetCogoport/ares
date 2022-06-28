package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

@JsonInclude
data class PayableAgeingBucket(
    val ageingKey: String,
    val ledgerAmount: BigDecimal = 0.toBigDecimal(),
    val breakup: List<DueAmount>? = null
)
