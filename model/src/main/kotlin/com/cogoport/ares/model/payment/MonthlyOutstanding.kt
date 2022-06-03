package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class MonthlyOutstanding(
    @JsonProperty("list")
    var list: List<OutstandingResponse>?,
    @JsonProperty("id")
    var id: String
)
