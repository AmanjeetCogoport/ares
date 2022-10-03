package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UploadSummary(
    @JsonProperty("errorFileUrlID")
    var errorFileUrlId: Long?,
    @JsonProperty("successCount")
    var successCount: Int,
    @JsonProperty("rejectedCount")
    var rejectedCount: Int,
)
