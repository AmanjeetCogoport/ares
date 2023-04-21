package com.cogoport.ares.model.settlement.request

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@Introspected
data class ParentJournalVoucherRequest(
    var id: String?,
    var jvCategory: String,
    var transactionDate: Date,
    var currency: String,
    var ledCurrency: String,
    var entityCode: Int,
    var entityId: UUID?,
    var jvCodeNum: String,
    var exchangeRate: BigDecimal,
    var description: String,
    var createdBy: UUID?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var jvLineItems: MutableList<JvLineItemRequest>
)
