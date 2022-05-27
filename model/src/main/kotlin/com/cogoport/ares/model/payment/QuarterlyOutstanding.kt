package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class QuarterlyOutstanding(
    @JsonProperty("response")
    var response: List<OutstandingResponse>,
    @JsonProperty("id")
    var id: String
)
