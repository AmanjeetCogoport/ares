package com.cogoport.ares.model.payment.event

import com.cogoport.ares.model.payment.DocumentStatus
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp
import java.time.Instant

@Introspected
data class UpdateInvoiceStatusRequest(
    var oldDocumentNo: Long,
    var newDocumentNo: Long,
    var docStatus: DocumentStatus?,
    var updatedAt: Timestamp = Timestamp.from(Instant.now()),
)
