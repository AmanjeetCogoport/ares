package com.cogoport.ares.api.common.enums

enum class SignSuffix(val sign: Short) {
    REC(-1),
    PAY(1),
    SALES_INVOICE(1),
    PURCHASE_INVOICE(-1),
    SALES_CREDIT_NOTE(-1),
    PURCHASE_CREDIT_NOTE(1),
    PURCHASE_DEBIT_NOTE(-1)
}
