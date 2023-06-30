package com.cogoport.ares.model.common

import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import java.math.BigDecimal

@MappedEntity
data class InvoiceBalanceResponse(
    @MappedProperty("document_value")
    var invoiceNumber: String,
    var amountCurr: BigDecimal,
    var payCurr: BigDecimal
)
