package com.cogoport.ares.api.settlement.entity

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID

@MappedEntity(value = "gl_code_masters")
data class GlCodeMaster(
    @field:Id @GeneratedValue
    val id: Long?,
    val accountCode: Int?,
    val description: String?,
    val ledAccount: String?,
    val accountType: String?,
    val classCode: Int?,
    val createdBy: UUID?,
    val updatedBy: UUID?,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    val createdAt: Timestamp?,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    val updatedAt: Timestamp?
)
