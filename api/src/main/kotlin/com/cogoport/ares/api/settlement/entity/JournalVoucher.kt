package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@Introspected
@MappedEntity(value = "journal_vouchers")
data class JournalVoucher(
    @field:Id @GeneratedValue
    var id: Long?,
    val entityId: UUID?,
    val entityCode: Int?,
    var jvNum: String,
    var type: String?,
    val category: JVCategory,
    val validityDate: Date?,
    val amount: BigDecimal?,
    val currency: String?,
    val ledCurrency: String,
    var status: JVStatus,
    val exchangeRate: BigDecimal?,
    val tradePartyId: UUID?,
    val tradePartyName: String,
    var createdBy: UUID?,
    var createdAt: Timestamp?,
    var updatedBy: UUID?,
    var updatedAt: Timestamp?,
    var description: String?,
    var accMode: AccMode,
    var parentJvId: Long? = null
)
