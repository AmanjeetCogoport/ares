package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity
data class OrgSummary(
    val orgId: UUID?,
    val orgName: String?,
    val outstanding: BigDecimal,
    val currency: String?,
)
