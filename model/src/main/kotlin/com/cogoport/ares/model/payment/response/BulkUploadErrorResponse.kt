package com.cogoport.ares.model.payment.response

import com.cogoport.brahma.excel.annotations.ExcelColumn
import com.cogoport.brahma.excel.annotations.ExcelSheet
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
@ExcelSheet(sheetName = "Sheet1")
data class BulkUploadErrorResponse(
    @ExcelColumn("organization_serial_id")
    var organizationSerialId: String,
    @ExcelColumn("trade_party_serial_id")
    var tradePartySerialId: String,
    @ExcelColumn("entity_code")
    var entityCode: String,
    @ExcelColumn("currency")
    var currency: String,
    @ExcelColumn("amount")
    var amount: String,
    @ExcelColumn("cogo_account_no")
    var cogoAccountNo: String,
    @ExcelColumn("exchange_rate")
    var exchangeRate: String,
    @ExcelColumn("ref_account_no")
    var refAccountNo: String,
    @ExcelColumn("pay_mode")
    var payMode: String,
    @ExcelColumn("payment_date")
    var paymentDate: String,
    @ExcelColumn("utr")
    var utr: String,
    @ExcelColumn("remarks")
    var remarks: String,
    @ExcelColumn("error_reason")
    var errorReason: String
)
// entity_code, trade_party_serial_id, organization_serial_id, currency, amount, cogo_account_no, ref_account_no, payment_date, utr, remarks
