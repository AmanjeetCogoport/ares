package com.cogoport.ares.model.payment.event

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.util.Date
import java.sql.Timestamp
import java.time.Instant

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UpdateInvoiceStatusRequest(
    var oldDocumentNo: Long,
    var accType: AccountType,
    var accMode: AccMode,
    var newDocumentNo: Long,
    var newDocumentValue: String?,
    var docStatus: DocumentStatus?,
    var updatedAt: Timestamp = Timestamp.from(Instant.now()),
    var transactionDate: Date?,
    var dueDate: Date?
)
