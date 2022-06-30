package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected

@Introspected
data class LedgerSummaryRequest(
    var startDate: String?,
    var endDate: String?,
    var orgId: String
)
