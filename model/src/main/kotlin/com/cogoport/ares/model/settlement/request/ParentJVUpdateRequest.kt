package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class ParentJVUpdateRequest(
    @field:NotNull(message = "parent jv id is required") val parentJvId: String?,
    val remark: String?,
    @field:NotNull(message = "status is mandatory") val status: JVStatus?,
    @field:NotNull(message = "performedBy is mandatory") val performedBy: UUID?
)
