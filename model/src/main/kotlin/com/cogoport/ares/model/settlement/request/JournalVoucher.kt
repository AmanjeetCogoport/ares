package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@Introspected
data class JournalVoucher(
    val entityCode: Int,
    val entityId: UUID?,
    val jvNum: String,
    val type: String,
    var status: JVStatus?,
    val category: JVCategory,
    val validityDate: Date,
    val amount: BigDecimal,
    val currency: String,
    val ledCurrency: String,
    val exchangeRate: BigDecimal,
    val tradePartyId: UUID,
    val tradePartnerName: String,
    var createdBy: UUID?,
)
