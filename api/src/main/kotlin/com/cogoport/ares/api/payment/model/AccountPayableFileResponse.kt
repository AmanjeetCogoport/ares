package com.cogoport.ares.api.payment.model

data class AccountPayableFileResponse(
    var documentNo: Long,
    var documentValue: String,
    var isSuccess: Boolean,
    var failureReason: String?
)
