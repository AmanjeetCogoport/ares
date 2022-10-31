package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
data class OverallStatsResponseData(
    @JsonProperty("totalOutstandingAmount")
    var totalOutstandingAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("openInvoicesCount")
    var openInvoicesCount: Int = 0,
    @JsonProperty("organizationCount")
    var organizationCount: Int = 0,
    @JsonProperty("openInvoicesAmount")
    var openInvoicesAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("openOnAccountPaymentAmount")
    var openOnAccountPaymentAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("dashboardCurrency")
    var dashboardCurrency: String
)
