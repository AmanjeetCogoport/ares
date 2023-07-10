package com.cogoport.ares.api.payment.model.requests

import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierReceivableRequest(
    var orgId: String,
    var entityCode: Int? = null,
    var currency: String
)
