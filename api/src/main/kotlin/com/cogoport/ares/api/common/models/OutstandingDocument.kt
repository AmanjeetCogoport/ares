package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class OutstandingDocument(
    var openInvoicesCount: Int? = 0,
    var openInvoiceAmount: BigDecimal = BigDecimal.ZERO,
    var currency: String?,
    var customersCount: Int? =0,
    var serviceType: String?,
    var groupedServices: String?,
    var tradeType: String?
)
