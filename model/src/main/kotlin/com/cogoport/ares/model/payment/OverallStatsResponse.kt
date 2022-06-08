package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
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
