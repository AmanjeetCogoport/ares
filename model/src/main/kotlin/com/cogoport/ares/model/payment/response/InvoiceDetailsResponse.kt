package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
@Introspected
data class InvoiceDetailsResponse(
    val documentValue: String?,
    val documentType: String?,
    val serviceType: String?,
    val invoiceAmount: BigDecimal?,
    val balance: BigDecimal?
)
