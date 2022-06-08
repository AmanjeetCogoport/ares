package com.cogoport.ares.model.payment.event

import com.cogoport.ares.model.payment.DocumentStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.sql.Timestamp
import java.time.Instant

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UpdateInvoiceStatusRequest(
    var oldDocumentNo: Long,
    var newDocumentNo: Long,
    var docStatus: DocumentStatus?,
    var updatedAt: Timestamp = Timestamp.from(Instant.now()),
)
