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
data class CustomerOutstanding(
    @JsonProperty("organizationId")
    var organizationId: String?,
    @JsonProperty("organizationName")
    var organizationName: String?,
    @JsonProperty("zoneCode")
    var zoneCode: String?,
    @JsonProperty("openInvoices")
    var openInvoices: InvoiceStats?,
    @JsonProperty("onAccountPayment")
    var onAccountPayment: InvoiceStats?,
    @JsonProperty("totalOutstanding")
    var totalOutstanding: InvoiceStats?,
    @JsonProperty("ageingBucket")
    var ageingBucket: List<AgeingBucket>?
)

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class InvoiceStats(
    @JsonProperty("invoicesCount")
    var invoicesCount: Int?,
    @JsonProperty("invoiceLedAmount")
    var invoiceLedAmount: BigDecimal?,
    @JsonProperty("amountDue")
    var amountDue: List<DueAmount>?
)

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DueAmount(
    @JsonProperty("currency")
    var currency: String?,
    @JsonProperty("amount")
    var amount: BigDecimal?,
    @JsonProperty("invoiceCount")
    var invoicesCount: Int?
)
