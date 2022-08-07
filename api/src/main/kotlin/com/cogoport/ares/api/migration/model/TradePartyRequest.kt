package com.cogoport.ares.api.migration.model
import io.micronaut.core.annotation.Introspected

@Introspected
data class TradePartyRequest(
    var organizationId: String
)
