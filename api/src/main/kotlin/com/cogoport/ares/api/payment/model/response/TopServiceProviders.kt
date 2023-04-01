package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import io.micronaut.core.annotation.Introspected

@Introspected
data class TopServiceProviders(
    var list: List<SupplierOutstandingDocument?>? = null,
    var currency: String? = "INR"
)
