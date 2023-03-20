package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class CustomerOutstandingList(
        @JsonProperty("list")
        val list: List<CustomersOutstanding?>? = null,
        @JsonProperty("totalPage")
        val totalPage: Int,
        @JsonProperty("totalRecords")
        val totalRecords: Int,
        @JsonProperty("page")
        val page: Int
)
