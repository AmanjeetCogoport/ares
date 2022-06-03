package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class OutstandingList(
    @JsonProperty("list")
    val list: MutableList<CustomerOutstanding?>,
    @JsonProperty("totalPage")
    val totalPage: Int,
    @JsonProperty("totalRecords")
    val totalRecords: Int
)
