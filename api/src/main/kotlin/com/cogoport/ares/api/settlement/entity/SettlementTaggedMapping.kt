package com.cogoport.ares.api.settlement.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@MappedEntity(value = "settlement_tagged_mappings")
data class SettlementTaggedMapping(
    @field:Id @GeneratedValue
    var id: Long?,
    var settlementId: Long,
    var utilizedSettlementId: Long,
    @DateCreated var createdAt: Timestamp? = null,
    var deletedAt: Timestamp? = null
)
