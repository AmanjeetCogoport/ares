package com.cogoport.ares.api.settlement.model

import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotNull

@Introspected
data class ICJVUpdateRequest(
    @field:NotNull(message = "incidentId is mandatory") val incidentId: String?,
    @field:NotNull(message = "parent jv id is required") val parentJvId: String?,
    val remark: String?,
    val status: JVStatus?,
    @field:NotNull(message = "performedBy is mandatory") val performedBy: UUID?
)
