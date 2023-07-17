package com.cogoport.ares.model.common

import com.cogoport.ares.model.payment.AccMode
import io.micronaut.core.annotation.Introspected

@Introspected
data class InvoiceBalanceAmountReq(
    var invoiceNumbers: List<String> ? = listOf(),
    var accMode: AccMode ? = AccMode.AR
)
