package com.cogoport.ares.model.payment.event

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UpdateInvoiceRequest(
    var documentNo: Long,
    var accMode: AccMode,
    var accType: AccountType,
    var docStatus: DocumentStatus,
    var entityCode: Int,
    var currency: String,
    var ledCurrency: String,
    var currAmount: BigDecimal,
    var ledAmount: BigDecimal,
    var dueDate: Timestamp,
    var transactionDate: Timestamp,
    var updatedAt: Timestamp = Timestamp.from(Instant.now()),
)
