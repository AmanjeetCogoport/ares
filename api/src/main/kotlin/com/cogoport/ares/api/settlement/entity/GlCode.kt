package com.cogoport.ares.api.settlement.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@Introspected
@MappedEntity(value = "gl_codes")
data class GlCode(
    @field:Id @GeneratedValue
    var id: Long?,
    var entityCode: Int,
    var accountNumber: String,
    var bankName: String?,
    var currency: String?,
    var glCode: String,
    var bankShortName: String?,
    @DateCreated var createdAt: Timestamp?
)
