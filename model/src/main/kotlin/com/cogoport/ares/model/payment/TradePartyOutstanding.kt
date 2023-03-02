package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TradePartyOutstanding(
    @JsonProperty("registrationNumber")
    var registrationNumber: String?,
    @JsonProperty("organizationId")
    var organizationId: String?,
    @JsonProperty("organizationName")
    var organizationName: String?,
    @JsonProperty("openInvoices")
    var openInvoices: InvoiceStats?,
    @JsonProperty("totalOutstanding")
    var totalOutstanding: InvoiceStats?,
    @JsonProperty("currency")
    var currency: String?
)
