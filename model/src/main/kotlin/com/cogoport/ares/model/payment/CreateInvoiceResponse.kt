package com.cogoport.ares.model.payment

data class CreateInvoiceResponse(

    var documentNo: Long,
    var isSuccess: Boolean,
    var message: String
)
