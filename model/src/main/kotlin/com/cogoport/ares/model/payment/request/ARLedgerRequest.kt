package com.cogoport.ares.model.payment.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import java.time.LocalDate
import java.util.UUID

@Introspected
data class ARLedgerRequest(
    @field:JsonFormat(pattern = "yyyy-MM-dd")
    var startDate: LocalDate?,
    @field:JsonFormat(pattern = "yyyy-MM-dd")
    var endDate: LocalDate?,
    var orgId: String,
    var orgName: String?,
    var entityCodes: List<Int>?,
    var requestedBy: UUID?
)
