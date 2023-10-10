package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.model.payment.response.SupplierOutstandingDocumentV2
import io.micronaut.core.annotation.Introspected

@Introspected
data class TopServiceProviders(
    var list: List<SupplierOutstandingDocumentV2?>? = null,
    var currency: String? = "INR"
)
