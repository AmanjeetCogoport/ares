package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@Introspected
data class JournalVoucher(
    val entityCode: Int,
    val jvNum: String,
    val type: String,
    val category: JVCategory,
    val validityDate: Date,
    val amount: BigDecimal,
    val currency: String,
    val ledCurrency: String,
    val status: JVStatus?,
    val exchangeRate: BigDecimal,
    val organizationId: UUID,
    val organizationName: String,
    val tradePartyMappingId: UUID,
    var createdBy: UUID?,
    var createdAt: Timestamp?,
    var updatedBy: UUID?,
    var updatedAt: Timestamp?,
)
