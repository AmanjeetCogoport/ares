package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class ServiceWiseRecPayResp(
    var service: String,
    var accountRec: BigDecimal? = 0.toBigDecimal(),
    var accountPay: BigDecimal? = 0.toBigDecimal(),
    var currency: String? = "INR"
)
