package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

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
    @JsonProperty("paymentsCount")
    val paymentsCount: Int?,
    @JsonProperty("paymentsAmount")
    val paymentsAmount: BigDecimal?,
    @JsonProperty("outstandingAmount")
    val outstandingAmount: BigDecimal?
)
