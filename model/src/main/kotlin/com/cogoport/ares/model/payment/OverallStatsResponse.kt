package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class OverallStatsResponse(
    @JsonProperty("totalOutstandingAmount")
    val totalOutstandingAmount: BigDecimal,
    @JsonProperty("openInvoicesCount")
    val openInvoicesCount: Int,
    @JsonProperty("organizationCount")
    val organizationCount: Int,
    @JsonProperty("openInvoicesAmount")
    val openInvoicesAmount: BigDecimal,
    @JsonProperty("openOnAccountPaymentAmount")
    val openOnAccountPaymentAmount: BigDecimal,
    @JsonProperty("id")
    var id: String?
)
