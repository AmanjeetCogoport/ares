package com.cogoport.ares.api.settlement.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@Introspected
@MappedEntity(value = "journal_voucher_codes")
data class JournalCode(
    @field:Id @GeneratedValue
    var id: Long?,
    var number: String,
    var description: String,
    var createdBy: UUID?,
    var updatedBy: UUID?,
    @DateCreated var createdAt: Timestamp?,
    @DateCreated var updatedAt: Timestamp?
)
