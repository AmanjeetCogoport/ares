package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
@MappedEntity
data class OrgStatsResponse(
    val organizationId: String,
    val currency: String,
    val receivables: BigDecimal?,
    val payables: BigDecimal?
)
