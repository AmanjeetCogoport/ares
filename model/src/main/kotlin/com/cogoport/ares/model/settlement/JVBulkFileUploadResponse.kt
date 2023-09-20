package com.cogoport.ares.model.settlement

import io.micronaut.core.annotation.Introspected

@Introspected
data class JVBulkFileUploadResponse(
    var errorFileUrl: String?
)
