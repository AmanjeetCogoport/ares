package com.cogoport.ares.model.settlement.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class JVBulkFileUploadRequest(
    @field:NotNull(message = "url is required")
    var url: String,
    @field:NotNull(message = "performedById is required")
    var performedById: UUID,
    var performedByUserType: String?
)
