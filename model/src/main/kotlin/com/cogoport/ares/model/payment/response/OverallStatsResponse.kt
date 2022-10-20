package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
data class OverallStatsResponse(
    @JsonProperty("totalOutstandingAmount")
    var totalOutstandingAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("openInvoicesCount")
    val openInvoicesCount: Int = 0,
    @JsonProperty("organizationCount")
    val organizationCount: Int = 0,
    @JsonProperty("openInvoicesAmount")
    var openInvoicesAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("openOnAccountPaymentAmount")
    var openOnAccountPaymentAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("id")
    var id: String?,
    @JsonProperty("dashboardCurrency")
    var dashboardCurrency: String?
)
