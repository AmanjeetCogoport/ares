package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected

@Introspected
data class ValidateTradePartyRequest(
    var serialId: String
)
