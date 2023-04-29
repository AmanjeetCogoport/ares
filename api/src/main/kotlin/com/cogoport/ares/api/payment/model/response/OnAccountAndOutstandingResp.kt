package com.cogoport.ares.api.payment.model.response

import java.math.BigDecimal

data class OnAccountAndOutstandingResp(
    var id: String,
    var value: BigDecimal? = 0.toBigDecimal()
)
