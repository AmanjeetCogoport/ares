package com.cogoport.ares.api.settlement.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@MappedEntity(value = "account_classes")
data class AccountClass(
    @field:Id @GeneratedValue
    val id: Long?,
    val ledAccount: String?,
    val accountCategory: String?,
    val classCode: Int?,
    val createdBy: UUID?,
    val updatedBy: UUID?,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?
)
