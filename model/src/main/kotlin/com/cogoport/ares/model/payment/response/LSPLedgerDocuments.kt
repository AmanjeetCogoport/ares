package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date

@Introspected
data class LSPLedgerDocuments(
    val transactionDate: Date,
    val ledgerCurrency: String,
    var shipmentId: String?,
    var documentNo: Long,
    var documentValue: String,
    var type: String,
    var debit: BigDecimal,
    var credit: BigDecimal,
    var debitBalance: BigDecimal?,
    var creditBalance: BigDecimal?,
    var balance: BigDecimal?
)
