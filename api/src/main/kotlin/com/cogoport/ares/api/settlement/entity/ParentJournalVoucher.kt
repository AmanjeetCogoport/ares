package com.cogoport.ares.api.settlement.entity

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
    var category: String,
    var jvNum: String?,
    var transactionDate: Date?,
    var validityDate: Date?,
    var currency: String?,
    var ledCurrency: String?,
    var entityCode: Int?,
    var exchangeRate: BigDecimal?,
    var description: String?,
    var createdBy: UUID?,
    var updatedBy: UUID?,
    var migrated: Boolean? = false,
    var jvCodeNum: String?,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    @DateCreated var createdAt: Timestamp? = null,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    @DateCreated var updatedAt: Timestamp? = null,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    var deletedAt: Timestamp? = null
)
