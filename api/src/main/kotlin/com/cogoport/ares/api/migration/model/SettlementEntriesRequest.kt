package com.cogoport.ares.api.migration.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class SettlementEntriesRequest(
    var startDate: String,
    var endDate: String,
    var entries: Map<String, String>
)
