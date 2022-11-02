package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.payment.AccountType
import io.micronaut.core.annotation.Introspected

@Introspected
data class InvoicePaymentRequest(
    var documentNo: Long,
    var accType: AccountType
)
