package com.cogoport.ares.model.payment

data class AccountCollectionResponse(
    val payments: List<Payment?>,
    val totalRecords: Int,
    val totalPage: Int
)
