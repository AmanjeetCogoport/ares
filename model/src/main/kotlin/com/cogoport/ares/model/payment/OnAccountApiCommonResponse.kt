package com.cogoport.ares.model.payment

data class OnAccountApiCommonResponse(
    var message: String,
    var id: Long,
    var isSuccess: Boolean
)
