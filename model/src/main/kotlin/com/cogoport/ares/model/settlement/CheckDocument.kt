package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccMode
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date

@JsonInclude
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Introspected
data class CheckDocument(
    var id: String,
    var documentNo: String,
    var documentValue: String,
    var accountType: SettlementType,
    var documentAmount: BigDecimal,
    var tds: BigDecimal?,
    var afterTdsAmount: BigDecimal,
    var balanceAmount: BigDecimal,
    var allocationAmount: BigDecimal,
    var currentBalance: BigDecimal?,
    var balanceAfterAllocation: BigDecimal,
    var ledgerAmount: BigDecimal,
    var status: String?,
    var settledAllocation: BigDecimal,
    var currency: String,
    var ledCurrency: String,
    var exchangeRate: BigDecimal,
    var transactionDate: Date,
    var settledTds: BigDecimal,
    var settledAmount: BigDecimal?,
    var nostroAmount: BigDecimal?,
    var settledNostro: BigDecimal?,
    var accMode: AccMode,
    var signFlag: Short
)
