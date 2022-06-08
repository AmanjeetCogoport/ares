package com.cogoport.ares.model.payment.event

import com.cogoport.ares.model.payment.DocumentStatus
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp
import java.time.Instant
import java.util.Date

@Introspected
data class UpdateInvoiceRequest(
    var documentNo: Long,
    var currency: String?,
    var docStatus: DocumentStatus?,
    var dueDate: Date?,
    var transactionDate: Date?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp = Timestamp.from(Instant.now()),
)
