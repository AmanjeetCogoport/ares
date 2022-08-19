package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

data class JournalVoucherApproval(
    var id: Long,
    var entityCode: Int,
    var entityId: UUID?,
    var jvNum: String,
    var type: String,
    var category: JVCategory,
    var validityDate: Date,
    var amount: BigDecimal,
    var currency: String,
    var ledCurrency: String,
    var status: JVStatus?,
    var exchangeRate: BigDecimal,
    var tradePartyId: UUID,
    var tradePartnerName: String,
    var createdBy: UUID?,
    var createdAt: Timestamp?,
    var updatedBy: UUID?,
    var updatedAt: Timestamp?,
)
