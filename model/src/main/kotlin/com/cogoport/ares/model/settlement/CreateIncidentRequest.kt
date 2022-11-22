package com.cogoport.ares.model.settlement

import io.micronaut.core.annotation.Introspected
import java.sql.Date
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class CreateIncidentRequest(
    var orgId: UUID?,
    var orgName: String?,
    @field:NotNull(message = "stackDetails is required") var stackDetails: List<CheckDocument>?,
    @field:NotNull(message = "createdBy is required") var createdBy: UUID?,
    @field:NotNull(message = "entityCode is required") var entityCode: Int?,
    @field:NotNull(message = "settlementDate is required") var settlementDate: Date?,
    val supportingDocUrl: String? = null
)
