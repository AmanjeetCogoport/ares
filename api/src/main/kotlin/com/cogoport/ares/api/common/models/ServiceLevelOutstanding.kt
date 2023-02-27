package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class ServiceLevelOutstanding(
    var totalOutstanding: BigDecimal?,
    var openInvoiceAmount: BigDecimal?,
    var onAccountPayment: BigDecimal?,
    var currency: String?,
    var tradeType: List<TradeAndServiceLevelOutstanding>?
)
