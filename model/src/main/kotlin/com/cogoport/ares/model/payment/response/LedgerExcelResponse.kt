package com.cogoport.ares.model.payment.response

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@ExcelSheet(sheetName = "Sheet1")
data class LedgerExcelResponse(
    @ExcelColumn("Transaction Date")
    val transactionDate: String,
    @ExcelColumn("Shipment ID")
    val shipmentId: String,
    @ExcelColumn("Document No/UTR")
    var documentValue: String,
    @ExcelColumn("Type")
    var type: String,
    @ExcelColumn("Ledger Currency")
    val ledgerCurrency: String,
    @ExcelColumn("Debit")
    var debit: BigDecimal,
    @ExcelColumn("Credit")
    var credit: BigDecimal,
    @ExcelColumn("Debit Balance")
    var debitBalance: BigDecimal?,
    @ExcelColumn("Credit Balance")
    var creditBalance: BigDecimal?,
    @ExcelColumn("Balance")
    var balance: BigDecimal?
)
