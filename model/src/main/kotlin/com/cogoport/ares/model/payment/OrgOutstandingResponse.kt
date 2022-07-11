package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class OrgOutstandingResponse(
    @JsonProperty("organizationId")
    val organizationId: String?,
    @JsonProperty("organizationName")
    val organizationName: String?,
    @JsonProperty("currency")
    val currency: String?,
    @JsonProperty("openInvoicesCount")
    val openInvoicesCount: Int?,
    @JsonProperty("openInvoicesAmount")
    val openInvoicesAmount: BigDecimal?,
    @JsonProperty("openInvoicesLedAmount")
    val openInvoicesLedAmount: BigDecimal?,
    @JsonProperty("paymentsCount")
    val paymentsCount: Int?,
    @JsonProperty("paymentsAmount")
    val paymentsAmount: BigDecimal?,
    @JsonProperty("paymentsLedAmount")
    val paymentsLedAmount: BigDecimal?,
    @JsonProperty("outstandingAmount")
    val outstandingAmount: BigDecimal?,
    @JsonProperty("outstandingLedAmount")
    val outstandingLedAmount: BigDecimal?
)
