package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.settlement.CheckDocument
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.validation.constraints.NotNull

data class CheckRequest(
    @field:NotNull(message = "stackDetails is mandatory") val stackDetails: MutableList<CheckDocument>?,
    val settlementDate: Timestamp = Timestamp.from(Instant.now()),
    @field:NotNull(message = "createdBy is mandatory")
    val createdBy: UUID?,
    @field:NotNull(message = "createdByUserType is mandatory")
    val createdByUserType: String?,
    val throughIncident: Boolean = false,
    val incidentId: String?,
    val incidentMappingId: String?,
    val remark: String?
)
