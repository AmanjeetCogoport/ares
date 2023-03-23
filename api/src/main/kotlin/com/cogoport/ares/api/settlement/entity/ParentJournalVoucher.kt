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
@MappedEntity(value = "parent_journal_vouchers")
data class ParentJournalVoucher(
    @field:Id @GeneratedValue
    var id: Long?,
    var status: JVStatus,
    var category: JVCategory,
    var jvNum: String?,
    var validityDate: Date?,
    var amount: BigDecimal?,
    var currency: String?,
    var ledCurrency: String?,
    var exchangeRate: BigDecimal?,
    var description: String?,
    var accMode: AccMode?,
    var createdBy: UUID?,
    var updatedBy: UUID?,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    @DateCreated var createdAt: Timestamp?,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    @DateCreated var updatedAt: Timestamp?,
)
