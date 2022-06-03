package com.cogoport.ares.model.payment

data class AccountPayableFileResponse(
    var documentNo: Long,
    var documentValue: String,
    var isSuccess: Boolean,
    var failureReason: String?
)
