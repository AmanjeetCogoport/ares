package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class UpdateSupplierOutstandingRequest(
    var orgId: UUID?,
    var entityCode: Int? = null
)
