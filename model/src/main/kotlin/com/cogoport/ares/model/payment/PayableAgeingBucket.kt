package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonInclude
@JsonInclude
data class PayableAgeingBucket(
    val ageingKey: String,
    val breakup: List<DueAmount>? = null
)
