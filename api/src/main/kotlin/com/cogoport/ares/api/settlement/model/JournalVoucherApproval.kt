package com.cogoport.ares.api.settlement.model

import com.cogoport.hades.model.incident.JournalVoucher
import io.micronaut.core.annotation.Introspected
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class JournalVoucherApproval(
    @field:NotNull(message = "incidentId is mandatory") val incidentId: String?,
    @field:NotNull(message = "journalVoucherData is mandatory") val journalVoucherData: JournalVoucher?,
    val remark: String?,
    @field:NotNull(message = "performedBy is mandatory") val performedBy: UUID?
)
