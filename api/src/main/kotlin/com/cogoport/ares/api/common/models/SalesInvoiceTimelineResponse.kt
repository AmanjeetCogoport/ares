package com.cogoport.ares.api.common.models


import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity


@Introspected
@MappedEntity
data class SalesInvoiceTimelineResponse(
    @JsonProperty("id")
    var id: Long,
    @JsonProperty("status")
    var status: String,
    @JsonProperty("payment_status")
    var paymentStatus: String?,
    @JsonProperty("events")
    var events: String
)