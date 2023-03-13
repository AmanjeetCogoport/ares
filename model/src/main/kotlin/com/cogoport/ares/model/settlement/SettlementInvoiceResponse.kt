package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.Date

@JsonInclude
class SettlementInvoiceResponse(
    var id: Long?,
    var invoiceNo: Long,
    var invoiceValue: String,
    var invoiceDate: Date,
    var dueDate: Date?,
    var invoiceAmount: BigDecimal?,
    var taxableAmount: BigDecimal?,
    var settledAmount: BigDecimal?,
    var balanceAmount: BigDecimal?,
    var status: String,
    var invoiceStatus: String,
    var currency: String,
    var sid: String?,
    var shipmentType: String? = null,
    var pdfUrl: String? = null,
)
