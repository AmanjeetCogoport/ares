package com.cogoport.ares.api.settlement.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@MappedEntity
data class InvoiceDocument(
    var id: Long,
    var documentNo: Long,
    var organizationId: UUID,
    var documentValue: String,
    var documentType: String,
    var accountType: String,
    var documentDate: Date,
    var dueDate: Date?,
    var documentAmount: BigDecimal,
    var documentLedAmount: BigDecimal,
    var taxableAmount: BigDecimal,
    var tds: BigDecimal,
    var tdsPercentage: BigDecimal? = null,
    var afterTdsAmount: BigDecimal,
    var settledAmount: BigDecimal,
    var balanceAmount: BigDecimal,
    var currentBalance: BigDecimal,
    var status: String?,
    var currency: String,
    var ledCurrency: String,
    var exchangeRate: BigDecimal,
    var signFlag: Short
)
