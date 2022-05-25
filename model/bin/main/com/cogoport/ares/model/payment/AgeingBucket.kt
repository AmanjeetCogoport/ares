package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class AgeingBucket(
    var ageingDuration: String?,
    var amount: BigDecimal?,
    var count: Int?,
    var ageingDurationKey: String?
)