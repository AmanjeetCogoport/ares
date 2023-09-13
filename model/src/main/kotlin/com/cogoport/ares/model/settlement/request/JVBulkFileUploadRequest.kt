package com.cogoport.ares.model.settlement.request

import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotNull

@Introspected
data class JVBulkFileUploadRequest(
    @field:NotNull(message = "url is required")
    var url: String,
    @field:NotNull(message = "user is required")
    var user: String,
    var userType: String?
)
