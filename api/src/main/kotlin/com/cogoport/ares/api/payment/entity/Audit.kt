package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.sql.Timestamp
import java.time.LocalDateTime

@MappedEntity("audits")
data class Audit(
    @field:Id @GeneratedValue var id: Long?,
    var objectType: String,
    var objectId: Long?,
    var actionName: String,
    @field:TypeDef(type = DataType.JSON)
    @JsonProperty("data")
    var data: Any?,
    var performedBy: String?,
    var performedByUserType: String?,
    @DateCreated var createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now())
)
