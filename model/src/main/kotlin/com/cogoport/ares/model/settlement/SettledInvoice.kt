package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@JsonInclude
data class SettledInvoice(
    var id: String?,
    val organizationId: UUID?,
    var documentNo: String,
    var documentValue: String,
    val documentType: SettlementType,
    var accountType: String,
    val currency: String?,
    val paymentCurrency: String?,
    var balanceAmount: BigDecimal,
    var currentBalance: BigDecimal,
    val documentAmount: BigDecimal,
    val ledCurrency: String,
    val ledgerAmount: BigDecimal,
    var taxableAmount: BigDecimal,
    var tds: BigDecimal,
    var afterTdsAmount: BigDecimal,
    var allocationAmount: BigDecimal? = BigDecimal.ZERO,
    var balanceAfterAllocation: BigDecimal? = BigDecimal.ZERO,
    var settledAmount: BigDecimal,
    var settledAllocation: BigDecimal? = BigDecimal.ZERO,
    var transactionDate: Date?,
    var status: String?,
    var settledTds: BigDecimal,
    var exchangeRate: BigDecimal,
    var signFlag: Short,
    var sid: String?,
    var nostroAmount: BigDecimal,
    var accMode: AccMode,
    var settlementStatus: SettlementStatus
)
