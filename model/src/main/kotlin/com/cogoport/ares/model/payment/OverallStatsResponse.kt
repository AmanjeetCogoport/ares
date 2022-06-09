package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
data class OverallStatsResponse(
    @JsonProperty("totalOutstandingAmount")
    val totalOutstandingAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("openInvoicesCount")
    val openInvoicesCount: Int = 0,
    @JsonProperty("organizationCount")
    val organizationCount: Int = 0,
    @JsonProperty("openInvoicesAmount")
    val openInvoicesAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("openOnAccountPaymentAmount")
    val openOnAccountPaymentAmount: BigDecimal = 0.toBigDecimal(),
    @JsonProperty("id")
    var id: String?
)
