package com.cogoport.ares.api.settlement.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@Introspected
@MappedEntity(value = "journal_voucher_categories")
data class JvCategory(
    @field:Id @GeneratedValue
    var id: Long?,
    var category: String,
    var description: String,
    @DateCreated var createdAt: Timestamp?,
    @DateCreated var updatedAt: Timestamp?
)
