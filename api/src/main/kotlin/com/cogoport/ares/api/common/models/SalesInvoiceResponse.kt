package com.cogoport.ares.api.common.models

import com.cogoport.plutus.model.invoice.enums.InvoiceStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class SalesInvoiceResponse(
    var id: Long?,
    var status: InvoiceStatus?,
    var paymentStatus: String?
)
