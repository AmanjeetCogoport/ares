package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.DueAmount
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
@JsonInclude
data class PayableOutstandingResponse(
    val currency: String?,
    val amount: BigDecimal?,
    val breakup: List<DueAmount>? = null
)
