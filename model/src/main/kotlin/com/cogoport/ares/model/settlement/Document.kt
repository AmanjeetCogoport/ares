package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.Date
@JsonInclude
data class Document(

    var id: Long,
    var documentNo: Long,
    var documentValue: String,
    var accountType: String,
    var documentType: String,
    var transactionDate: Date,
    var dueDate: Date?,
    var documentAmount: BigDecimal,
    var ledgerAmount: BigDecimal,
    var taxableAmount: BigDecimal,
    var tds: BigDecimal,
    var afterTdsAmount: BigDecimal,
    var allocationAmount: BigDecimal?,
    var balanceAfterAllocation: BigDecimal?,
    var settledAmount: BigDecimal,
    var settledAllocation: BigDecimal?,
    var balanceAmount: BigDecimal,
    var status: String?,
    var currency: String,
    var ledCurrency: String,
    var settledTds: BigDecimal?,
    var exchangeRate: BigDecimal,
    var signFlag: Short

)
