package com.cogoport.ares.api.settlement.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.LocalDateTime

@Introspected
@MappedEntity(value = "third_party_api_audits")
data class ThirdPartyApiAudit (
    @field:Id
    @GeneratedValue
    var id: Long?,
    var apiName: String,
    var apiType: String,
    var objectId: Long?,
    var objectName: String?,
    var httpResponseCode: String?,
    var requestParams: String,
    var response: String,
    var isSuccess: Boolean,
    @DateCreated
    var createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    @DateUpdated
    var updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now())
)