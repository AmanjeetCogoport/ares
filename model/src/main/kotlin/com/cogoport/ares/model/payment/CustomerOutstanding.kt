package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

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

data class InvoiceStats(
    @JsonProperty("invoicesCount")
    var invoicesCount: Int?,
    @JsonProperty("amountDue")
    var amountDue: List<DueAmount>?
)

data class DueAmount(
    @JsonProperty("currency")
    var currency: String?,
    @JsonProperty("amount")
    var amount: BigDecimal?,
    @JsonProperty("invoiceCount")
    var invoicesCount: Int?
)
