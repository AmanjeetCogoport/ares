package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.Date

@JsonInclude
data class SettledInvoice(
    val id: Long?,
    val documentNo: Long,
    var documentValue: String,
    val documentType: SettlementType,
    var accountType: String,
    val invoiceCurrency: String?,
    val paymentCurrency: String?,
    var balanceAmount: BigDecimal,
    var currentBalance: BigDecimal,
    val documentAmount: BigDecimal?,
    val ledCurrency: String,
    val ledgerAmount: BigDecimal,
    var taxableAmount: BigDecimal,
    var tds: BigDecimal,
    var afterTdsAmount: BigDecimal,
    var allocationAmount: BigDecimal? = BigDecimal.ZERO,
    var balanceAfterAllocation: BigDecimal? = BigDecimal.ZERO,
    var settledAmount: BigDecimal? = BigDecimal.ZERO,
    var settledAllocation: BigDecimal? = BigDecimal.ZERO,
    val transactionDate: Date,
    var status: String?,
    var settledTds: BigDecimal? = BigDecimal.ZERO,
    var exchangeRate: BigDecimal,
    var signFlag: Short,
    var sid: Long?
)
