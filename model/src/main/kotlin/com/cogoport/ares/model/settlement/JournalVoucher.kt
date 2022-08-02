package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.settlement.enums.JVCategory
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@JsonInclude
data class JournalVoucher(
    var id: Long?,
    val entityCode: Int?,
    val entityId: Int?,
    val jvNum: String?,
    val type: String?,
    val category: JVCategory?,
    val validityDate: Date?,
    val amount: BigDecimal?,
    val currency: String?,
    val tradePartyId: UUID?,
    val tradePartnerName: String?,
    var createdBy: UUID?,
    var createdAt: Timestamp?,
    var updatedBy: UUID?,
    var updatedAt: Timestamp?,
)
