package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.*

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
