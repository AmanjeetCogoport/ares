package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.InvoiceStatus
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
@JsonInclude
data class CheckDocument(
    var id: Long,
    var documentNo: String?,
    var accountType: AccountType,
    var invoiceAmount: BigDecimal = 0.toBigDecimal(),
    var tds: BigDecimal?,
    var afterTdsAmount: BigDecimal = 0.toBigDecimal(),
    var balanceAmount: BigDecimal = 0.toBigDecimal(),
    var allocationAmount: BigDecimal?,
    var balanceAfterAllocation: BigDecimal?,
    var documentStatus: InvoiceStatus?,
    var signFlag: Int,
    var settledAmount: BigDecimal = 0.toBigDecimal()
)
