package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class QuarterlyOutstanding(
    @JsonProperty("response")
    var response: MutableList<OutstandingResponse>,
    @JsonProperty("docKey")
    var docKey: String
)
