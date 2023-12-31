package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.Date

@JsonInclude
data class Invoice(
    var id: Long?,
    var invoiceNo: Long,
    var invoiceValue: String,
    var invoiceDate: Date,
    var dueDate: Date?,
    var invoiceAmount: BigDecimal?,
    var taxableAmount: BigDecimal?,
    var tds: BigDecimal?,
    var afterTdsAmount: BigDecimal?,
    var settledAmount: BigDecimal?,
    var balanceAmount: BigDecimal?,
    var status: String
)
