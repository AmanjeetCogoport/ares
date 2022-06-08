package com.cogoport.ares.model.payment.event

import com.cogoport.ares.model.payment.DocumentStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.sql.Timestamp
import java.time.Instant
import java.util.Date

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UpdateInvoiceRequest(
    var documentNo: Long,
    var currency: String?,
    var docStatus: DocumentStatus?,
    var dueDate: Date?,
    var transactionDate: Date?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp = Timestamp.from(Instant.now()),
)
