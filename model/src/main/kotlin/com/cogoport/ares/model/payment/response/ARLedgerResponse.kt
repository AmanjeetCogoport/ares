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
    @ExcelColumn("Document Type")
    val documentType: String?,
    @ExcelColumn("Document Number")
    val documentNumber: String,
    @ExcelColumn("Original Transaction Currency")
    val currency: String?,
    @ExcelColumn("Original Transaction Amount")
    val amount: String?,
    @ExcelColumn("Ledger Debit")
    val debit: BigDecimal,
    @ExcelColumn("Ledger Credit")
    val credit: BigDecimal,
    @ExcelColumn("Debit Balance")
    var debitBalance: BigDecimal,
    @ExcelColumn("Credit Balance")
    var creditBalance: BigDecimal,
    @ExcelColumn("Transaction Ref Number")
    val transactionRefNumber: String?,
    @ExcelColumn("MAWB/HAWB/MBL/HBL")
    val shipmentDocumentNumber: String?
)
