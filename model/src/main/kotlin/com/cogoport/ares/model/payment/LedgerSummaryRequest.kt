package com.cogoport.ares.model.payment

import java.util.UUID

data class LedgerSummaryRequest(
    var startDate: String?,
    var endDate: String?,
    var orgId: String
)