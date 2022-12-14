package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class InvoiceListResponse(
    val organizationId: String?,
    val documentValue: String?,
    val documentType: String?,
    val serviceType: String?,
    val invoiceAmount: BigDecimal?,
    val balance: BigDecimal?
)
