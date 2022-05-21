package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.api.payment.model.AgeingBucket
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
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
