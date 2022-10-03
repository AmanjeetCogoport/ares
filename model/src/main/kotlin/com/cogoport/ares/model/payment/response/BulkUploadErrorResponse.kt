package com.cogoport.ares.model.payment.response

data class BulkUploadErrorResponse(
    var organizationSerialId: String,
    var tradePartySerialId: String,
    var entityCode: String,
    var currency: String,
    var amount: String,
    var cogoAccountNo: String,
    var exchangeRate: String,
    var refAccountNo: String,
    var paymentDate: String,
    var utr: String,
    var remarks: String,
    var errorReason: String
)
// entity_code, trade_party_serial_id, organization_serial_id, currency, amount, cogo_account_no, ref_account_no, payment_date, utr, remarks
