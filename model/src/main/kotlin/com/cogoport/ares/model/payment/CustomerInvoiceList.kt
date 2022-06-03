package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class CustomerInvoiceList (
    @JsonProperty("list")
    val list: MutableList<CustomerInvoiceResponse?>,
    @JsonProperty("page")
    val page: Int,
    @JsonProperty("totalPage")
    val totalPage: Int,
    @JsonProperty("totalRecords")
    val totalRecords: Int
)