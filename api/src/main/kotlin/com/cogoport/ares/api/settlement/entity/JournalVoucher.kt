package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@MappedEntity(value = "journal_vouchers")
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
    val status: JVStatus,
    val exchangeRate: BigDecimal?,
    val tradePartyId: UUID?,
    val tradePartnerName: String?,
    var createdBy: UUID?,
    var createdAt: Timestamp?,
    var updatedBy: UUID?,
    var updatedAt: Timestamp?,
)
