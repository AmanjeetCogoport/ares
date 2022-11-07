package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("default_business_partners")
data class DefaultBusinessPartners(
    @field:Id @GeneratedValue var id: Long?,
    var businessName: String,
    var tradePartyDetailSerialId: String,
    var sageOrgId: String?,
    var tradePartyDetailId: UUID,
    var createdAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    var updatedAt: Timestamp? = Timestamp.valueOf(LocalDateTime.now()),
    var deletedAt: Timestamp?
)
