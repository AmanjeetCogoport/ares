package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty

data class MonthlyOutstanding(
    @JsonProperty("response")
    var response: MutableList<OutstandingResponse>?,
    @JsonProperty("docKey")
    var docKey: String
)
