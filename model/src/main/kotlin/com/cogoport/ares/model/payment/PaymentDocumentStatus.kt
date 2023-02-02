package com.cogoport.ares.model.payment

enum class PaymentDocumentStatus(val dbValue: String) {
    CREATED("CREATED"),
    APPROVED("APPROVED"),
    POSTED("POSTED"),
    POSTING_FAILED("POSTING_FAILED")
}