package com.cogoport.ares.model.payment

data class LedgerSummaryRequest(
    var startDate: String?,
    var endDate: String?,
    var orgId: String
)
