package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class OverallStatsResponse(
    @JsonProperty
    val totalOutstandingAmount: BigDecimal,
    @JsonProperty
    val openInvoicesCount: Int,
    @JsonProperty
    val organizationCount: Int,
    @JsonProperty
    val openInvoicesAmount: BigDecimal,
    @JsonProperty
    val openOnAccountPaymentAmount: BigDecimal,
    @JsonProperty
    var id: String?
)
