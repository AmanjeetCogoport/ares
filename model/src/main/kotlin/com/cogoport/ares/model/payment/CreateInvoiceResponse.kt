package com.cogoport.ares.model.payment

data class CreateInvoiceResponse(
    var id: Long,
    var documentNo: Long,
    var isSuccess: Boolean,
    var message: String
)
