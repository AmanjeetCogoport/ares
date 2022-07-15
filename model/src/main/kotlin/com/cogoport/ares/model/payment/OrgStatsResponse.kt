package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
@JsonInclude
data class OrgStatsResponse(
    val organizationId: String,
    val receivables: BigDecimal?,
    val currency: String,
    val payables: BigDecimal?
)
