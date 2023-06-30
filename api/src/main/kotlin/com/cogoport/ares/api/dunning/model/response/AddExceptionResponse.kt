package com.cogoport.ares.api.dunning.model.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AddExceptionResponse(
    var errorFileUrl: String?,
    var successCount: Int,
    var rejectedCount: Int
)
