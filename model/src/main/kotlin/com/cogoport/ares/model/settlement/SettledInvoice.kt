package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.Date

@JsonInclude
data class SettledInvoice(
    val id: Long?,
    val documentNo: Long, // change mapper
    var documentValue: String, // add in mapper
    val documentType: SettlementType, // reame in mapper
    var accountType: String, // add mapper
    val currency: String?,
    val balanceAmount: BigDecimal,
    val documentAmount: BigDecimal?, // rename in mapper amount to document amount
    val ledCurrency: String,
    val ledgerAmount: BigDecimal, // ledAmount -> ledgerAmount
    var taxableAmount: BigDecimal, // new added
    var tds: BigDecimal, // new added
    var afterTdsAmount: BigDecimal,
    var allocationAmount: BigDecimal? = BigDecimal.ZERO,
    var balanceAfterAllocation: BigDecimal? = BigDecimal.ZERO,
    var settledAmount: BigDecimal? = BigDecimal.ZERO,
    var settledAllocation: BigDecimal? = BigDecimal.ZERO,
    val transactionDate: Date, // rename mapper
    var status: String?,
    var settledTds: BigDecimal? = BigDecimal.ZERO,
    var exchangeRate: BigDecimal,
    var signFlag: Short
)
