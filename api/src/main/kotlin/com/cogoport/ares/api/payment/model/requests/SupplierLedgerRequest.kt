package com.cogoport.ares.api.payment.model.requests

import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierLedgerRequest(
        var orgId: String,
        var month: String? = "Jan",
        var q: String? = null
)
