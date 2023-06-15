package com.cogoport.ares.api.dunning.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID
import javax.persistence.GeneratedValue

@MappedEntity(value = "dunning_email_audits")
data class DunningEmailAudit(
    @field:Id @GeneratedValue
    var id: Long?,
    var executionId: Long,
    var tradePartyDetailId: UUID,
    var communicationId: UUID?,
    var isSuccess: Boolean,
    var errorReason: String?,
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis())
)
