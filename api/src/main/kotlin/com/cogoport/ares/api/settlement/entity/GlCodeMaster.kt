package com.cogoport.ares.api.settlement.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@MappedEntity(value = "gl_code_masters")
data class GlCodeMaster(
    @field:Id @GeneratedValue
    var id: Long?,
    val accountCode: Int?,
    val description: String?,
    val ledAccount: String?,
    val accountType: String?,
    val classCode: Int?,
    val accountClassId: Long,
    val createdBy: UUID?,
    val updatedBy: UUID?,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?
)
