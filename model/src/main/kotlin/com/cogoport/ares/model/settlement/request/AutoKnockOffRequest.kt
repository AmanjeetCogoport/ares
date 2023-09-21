package com.cogoport.ares.model.settlement.request

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AutoKnockOffRequest(
    var paymentIdAsSourceId: String,
    var destinationId: String,
    var sourceType: String? = "REC",
    var destinationType: String? = "SINV",
    var createdBy: UUID
)