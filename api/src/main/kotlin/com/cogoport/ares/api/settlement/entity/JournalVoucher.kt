package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
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
    var category: JVCategory,
    var validityDate: Date?,
    var amount: BigDecimal?,
    var currency: String?,
    var ledCurrency: String,
    var status: JVStatus,
    var exchangeRate: BigDecimal?,
    val tradePartyId: UUID?,
    val tradePartyName: String,
    var createdBy: UUID?,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    @DateCreated var createdAt: Timestamp?,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    @DateCreated var updatedAt: Timestamp?,
    var updatedBy: UUID?,
    var description: String?,
    var accMode: AccMode?,
    var glCode: String?,
    var parentJvId: Long? = null
)
