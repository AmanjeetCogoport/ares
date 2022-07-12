package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.InvoiceStatus
import com.cogoport.ares.model.payment.InvoiceType
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.Date

@JsonInclude
data class Document(
    var id: Long?,
    var documentNo: String?,
    var documentType: InvoiceType,
    var documentDate: Date?,
    var dueDate: Date?,
    var documentAmount: BigDecimal?,
    var taxableAmount: BigDecimal?,
    var tds: BigDecimal?,
    var afterTdsAmount: BigDecimal?,
    var settledAmount: BigDecimal?,
    var balanceAmount: BigDecimal?,
    var status: InvoiceStatus
)
