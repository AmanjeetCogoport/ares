package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

@JsonInclude
data class OrgStatsResponseForCoeFinance(
    val organizationId: String,
    val receivables: BigDecimal?,
    val receivablesCurrency: String?,
    val payablesCurrency: String?,
    val payables: BigDecimal?
)
