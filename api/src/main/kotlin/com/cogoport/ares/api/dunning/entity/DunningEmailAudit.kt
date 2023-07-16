package com.cogoport.ares.api.dunning.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp
import java.util.UUID
import javax.persistence.GeneratedValue

@MappedEntity(value = "dunning_email_audits")
data class DunningEmailAudit(
    @field:Id @GeneratedValue var id: Long?,
    var executionId: Long? = null,
    var communicationId: UUID? = null,
    var emailRecipients: String? = null,
    var userId: UUID? = null,
    var tradePartyDetailId: UUID? = null,
    var organizationId: UUID? = null,
    var isSuccess: Boolean? = false,
    var errorReason: String? = null,
    @DateCreated var createdAt: Timestamp? = null
)
