package com.cogoport.ares.model.settlement.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class SassSettlementRequest(
    var sourceId: String,
    var destinationId: String,
    var sourceType: String,
    var destinationType: String
)
