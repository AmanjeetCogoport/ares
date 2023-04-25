package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccMode
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date

@Introspected
@JsonInclude
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class HistoryDocument(
    var id: String?,
    var documentNo: String,
    val documentValue: String,
    val accountType: String?,
    val currency: String?,
    val balanceAmount: BigDecimal?,
    val documentAmount: BigDecimal?,
    val ledCurrency: String,
    val ledgerAmount: BigDecimal,
    val tds: BigDecimal?,
    val afterTdsAmount: BigDecimal?,
    val allocationAmount: BigDecimal?,
    val balanceAfterAllocation: BigDecimal?,
    val settledAllocation: BigDecimal?,
    val taxableAmount: BigDecimal,
    val settledTds: BigDecimal?,
    val transactionDate: Date,
    var signFlag: Short,
    val exchangeRate: BigDecimal,
    val settledAmount: BigDecimal,
    val lastEditedDate: Date,
    var status: String?,
    var accMode: AccMode,
    var supportingDocUrl: String? = null,
    var notPostedSettlementIds: MutableList<Long>? = null
)
