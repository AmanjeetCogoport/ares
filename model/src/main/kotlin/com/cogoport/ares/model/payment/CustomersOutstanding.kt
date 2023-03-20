package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class CustomersOutstanding(
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
        var ageingBucket: List<AgeingBucket>?,
        @JsonProperty("creditNoteCount")
        var creditNoteCount: Int,
        @JsonProperty("totalCreditAmount")
        var totalCreditAmount: BigDecimal?,
        @JsonProperty("debitNoteCount")
        var debitNoteCount: Int,
        @JsonProperty("totalDebitAmount")
        var totalDebitAmount: BigDecimal?
)
