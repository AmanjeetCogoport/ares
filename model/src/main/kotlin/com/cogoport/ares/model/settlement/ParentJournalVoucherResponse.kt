package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Introspected
data class ParentJournalVoucherResponse(
    var id: String,
    var status: JVStatus?,
    var category: String?,
    var jvNum: String?,
    var jvCodeNum: String?,
    var entityCode: Int?,
    var currency: String?,
    var ledCurrency: String?,
    var exchangeRate: BigDecimal?,
    var description: String?,
    var createdBy: UUID?,
    var createdByName: String?,
    var updatedBy: UUID?,
    var validityDate: Date?,
    var transactionDate: Date?,
    var isUtilized: Boolean?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp? = Timestamp.from(Instant.now())
)
