package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
@JsonInclude
data class PayableOutstandingResponse(
    val currency: String?,
    val amount: BigDecimal?,
    val breakout: List<DueAmount>? = null
)
