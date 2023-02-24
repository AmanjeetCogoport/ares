package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class DocumentTdsRequest(
    var documentNo: String,
    val paidTds: BigDecimal,
    val payableTds: BigDecimal,
    val transactionNo: String? = null,
    val transferType: String? = null,
    val exchangeRate: BigDecimal
)
