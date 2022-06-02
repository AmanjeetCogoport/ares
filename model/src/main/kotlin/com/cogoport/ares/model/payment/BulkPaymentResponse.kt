package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty

data class BulkPaymentResponse(
    @JsonProperty("recordsInserted")
    var recordsInserted: Int,
)
