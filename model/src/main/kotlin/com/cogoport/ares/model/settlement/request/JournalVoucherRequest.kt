package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@Introspected
data class JournalVoucherRequest(
    var id: String?,
    val entityCode: Int,
    var entityId: UUID?,
    var jvNum: String?,
    val type: String,
    var status: JVStatus?,
    var category: JVCategory,
    val validityDate: Date,
    val amount: BigDecimal,
    val currency: String,
    val ledCurrency: String,
    val exchangeRate: BigDecimal,
    val tradePartyId: UUID,
    var tradePartyName: String?,
    var createdBy: UUID?,
    var accMode: AccMode,
    var description: String?,
    var parentJvId: String? = null,
)
