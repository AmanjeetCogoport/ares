package com.cogoport.ares.model.payment.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp

@Introspected
data class LedgerSummaryRequest(
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var startDate: Timestamp?,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var endDate: Timestamp?,
    var orgId: String
)
