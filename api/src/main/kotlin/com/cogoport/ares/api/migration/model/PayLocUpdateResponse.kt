package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class PayLocUpdateResponse(
    @JsonProperty("acc_type")
    val accType: String?,
    @JsonProperty("document_no")
    val documentNo: Long?,
)
