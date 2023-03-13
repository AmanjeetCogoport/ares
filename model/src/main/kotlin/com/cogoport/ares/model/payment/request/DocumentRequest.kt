package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class DocumentRequest(
    var documentNo: String,
    val exchangeRate: BigDecimal,
    val transferType: String
)
