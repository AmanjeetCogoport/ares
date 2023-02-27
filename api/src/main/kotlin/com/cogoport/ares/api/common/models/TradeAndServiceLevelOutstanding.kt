package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class TradeAndServiceLevelOutstanding(
    var name: String?,
    var key: String?,
    val totalOutstanding: BigDecimal = BigDecimal.ZERO,
    val openInvoiceAmount: BigDecimal = BigDecimal.ZERO,
    var onAccountPayment: BigDecimal = BigDecimal.ZERO,
    var currency: String?
)
