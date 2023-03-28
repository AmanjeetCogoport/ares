package com.cogoport.ares.api.migration.entity

import com.cogoport.ares.model.settlement.enums.JVStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@Introspected
@MappedEntity(value = "parent_journal_vouchers")
data class ParentJournalVoucherMigration(
    @field:Id @GeneratedValue
    var id: Long?,
    var status: JVStatus,
    var category: String,
    var jvNum: String?,
    var validityDate: Date?,
    var createdBy: UUID?,
    var updatedBy: UUID?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp? = Timestamp.from(Instant.now()),
    var currency: String?,
    var led_currency: String?,
    var amount: BigDecimal?,
    var exchangeRate: BigDecimal,
    var description: String?,
    var migrated: Boolean? = false
)
