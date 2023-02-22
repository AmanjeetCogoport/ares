package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.sql.Timestamp
import java.util.*

@MappedEntity
@Introspected
data class InvoiceEventResponse(
    var id: Long,
    var invoiceId: Long,
    var eventName: String,
    var createdAt: Timestamp,
    var updatedAt: Timestamp,
    var occurredAt: Timestamp
)
