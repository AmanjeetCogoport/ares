package com.cogoport.ares.model.settlement.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class JournalVoucherReject(
    @field:NotNull(message = "incidentId is mandatory") val incidentId: String?,
    @field:NotNull(message = "performedBy is mandatory") val performedBy: UUID?,
    @field:NotNull(message = "journalVoucherId is mandatory") val journalVoucherId: String?,
    val remark: String?,
)
