package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccountType
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
    var accountType: AccountType,
    var documentDate: Date,
    var dueDate: Date?,
    var documentAmount: BigDecimal,
    var taxableAmount: BigDecimal,
    var settledAmount: BigDecimal,
    var balanceAmount: BigDecimal,
    var currency: String,
    var invoiceStatus: String,
    var settledTds: BigDecimal
)
