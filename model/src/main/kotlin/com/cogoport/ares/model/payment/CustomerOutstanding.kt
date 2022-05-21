package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class CustomerOutstanding(
    @JsonProperty("organizationName")
    var organizationName: String,
    @JsonProperty("openInvoices")
    var openInvoices: InvoiceStats,
    @JsonProperty("onAccountPayment")
    var onAccountPayment: InvoiceStats,
    @JsonProperty("totalOutstanding")
    var totalOutstanding: InvoiceStats,
    @JsonProperty("ageingBucket")
    var ageingBucket: MutableList<AgeingBucket>
)

data class InvoiceStats(
    @JsonProperty("invoicesCount")
    var invoicesCount: Int,
    @JsonProperty("amountDue")
    var amountDue: MutableList<DueAmount>
)

data class DueAmount(
    @JsonProperty("currency")
    var currency: String,
    @JsonProperty("amount")
    var amount: BigDecimal
) {
    init {
        currency = "INR"
        amount = 0.toBigDecimal()
    }
}
