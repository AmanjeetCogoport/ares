package com.cogoport.ares.api.payment.model.requests

import io.micronaut.core.annotation.Introspected

@Introspected
data class BfServiceWiseOverdueReq(
    var entityCode: Int? = null,
    var interfaceType: String,
    var tradeType: String? = null
)
