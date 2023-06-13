package com.cogoport.ares.api.payment.model.requests

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected

@Introspected
data class BfServiceWiseOverdueReq(
    var entityCode: MutableList<Int>? = mutableListOf(101, 301),
    var interfaceType: String? = "ocean",
    var tradeType: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var startDate: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var endDate: String? = null
)
