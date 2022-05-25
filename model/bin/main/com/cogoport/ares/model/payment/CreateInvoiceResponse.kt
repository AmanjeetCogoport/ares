package com.cogoport.ares.model.payment

data class CreateInvoiceResponse(

    var documentNo: Long,
    var isSucess: Boolean,
    var message: String
)
