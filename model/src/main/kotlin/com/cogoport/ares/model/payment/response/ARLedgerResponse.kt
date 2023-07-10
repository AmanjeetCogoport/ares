package com.cogoport.ares.model.payment.response

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
@ExcelSheet(sheetName = "Sheet1")
data class ARLedgerResponse(
    @ExcelColumn("Transaction Date")
    val transactionDate: String?,
    @ExcelColumn("Document Number")
    val documentNumber: String?,
    @ExcelColumn("Document Value")
    val documentValue: String,
    @ExcelColumn("Transaction Ref Number")
    val transactionRefNumber: String?,
    @ExcelColumn("Ledger Currency")
    val ledgerCurrency: String?,
    @ExcelColumn("Debit")
    val debit: BigDecimal,
    @ExcelColumn("Credit")
    val credit: BigDecimal,
    @ExcelColumn("Balance")
    val balance: BigDecimal
)
