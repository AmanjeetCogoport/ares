package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccountType
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@JsonInclude
@Introspected
data class InvoiceDocumentResponse(
    var id: Long,
    var organizationId: UUID,
    var accountType: AccountType,
    var documentNo: Long,
    var documentValue: String,
    var documentDate: Date,
    var dueDate: Date?,
    var documentAmount: BigDecimal,
    var taxableAmount: BigDecimal,
    var tds: BigDecimal?,
    var tdsPercentage: BigDecimal?,
    var afterTdsAmount: BigDecimal?,
    var settledAmount: BigDecimal,
    var balanceAmount: BigDecimal,
    var status: String?,
    var currency: String,
    var invoiceStatus: String,
    var settledTds: BigDecimal
)
