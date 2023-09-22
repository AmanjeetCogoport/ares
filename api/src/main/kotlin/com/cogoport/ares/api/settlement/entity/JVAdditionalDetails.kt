package com.cogoport.ares.api.settlement.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
data class JVAdditionalDetails(
    val utr: String?,
    val bpr: String?,
    val aresDocumentId: Long?
)
