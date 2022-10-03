package com.cogoport.ares.model.payment.request

import java.util.UUID
import javax.validation.constraints.NotNull

data class BulkUploadRequest(
    @field: NotNull(message = "File Url is required")
    var fileUrl: String,
    @field: NotNull(message = "Uploaded By is required")
    var uploadedBy: UUID
)

