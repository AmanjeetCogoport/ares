package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp

@Introspected
data class InvoiceEventResponse(
    @JsonProperty("id")
    var id: Long,
    @JsonProperty("invoice_id")
    val invoiceId: Long,
    @JsonProperty("event_name")
    val eventName: String,
    @JsonProperty("occurred_at")
    val occurredAt: Timestamp,
)
