package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccMode
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@JsonInclude
data class Document(

    var id: String,
    var documentNo: String,
    var documentValue: String,
    var organizationId: UUID,
    var accountType: String,
    var documentType: String,
    var transactionDate: Date,
    var mappingId: UUID?,
    var dueDate: Date?,
    var documentAmount: BigDecimal,
    var ledgerAmount: BigDecimal,
    var ledgerBalance: BigDecimal,
    var taxableAmount: BigDecimal,
    var tds: BigDecimal,
    var tdsPercentage: BigDecimal? = null,
    var afterTdsAmount: BigDecimal,
    var allocationAmount: BigDecimal?,
    var balanceAfterAllocation: BigDecimal?,
    var settledAmount: BigDecimal,
    var settledAllocation: BigDecimal?,
    var balanceAmount: BigDecimal,
    var currentBalance: BigDecimal,
    var status: String?,
    var currency: String,
    var ledCurrency: String,
    var settledTds: BigDecimal?,
    var exchangeRate: BigDecimal,
    var signFlag: Short,
    var nostroAmount: BigDecimal,
    var approved: Boolean?,
    var accMode: AccMode
)
