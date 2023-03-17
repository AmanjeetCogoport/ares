package com.cogoport.ares.api.settlement.entity

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@Introspected
@MappedEntity(value = "gl_codes")
data class GLCode(
    @field:Id @GeneratedValue
    var id: Long?,
    var entityCode: Int,
    var accountNumber: String,
    var bankName: String?,
    var currency: String?,
    var glCode: String,
    var bankShortName: String?,
    @field:JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "Asia/Kolkata")
    @DateCreated var createdAt: Timestamp?
)
