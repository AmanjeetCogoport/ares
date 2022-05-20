package com.cogoport.ares.payment.model

data class CreateInvoiceResponse (

    var documentNo:Long,
    var isSucess:Boolean,
    var message:String
)