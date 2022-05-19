package com.cogoport.ares.payment.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class OverallStats(
    @JsonProperty("openInvoiceCount")
    var openInvoiceCount: Int,
    @JsonProperty("openInvoiceAmount")
    var openInvoiceAmount: BigDecimal,
    @JsonProperty("onAccountPayment")
    var onAccountPayment: BigDecimal,
    @JsonProperty("accountReceivables")
    var accountReceivables: BigDecimal,
    @JsonProperty("organizations")
    var organizations: Int,
    @JsonProperty("docKey")
    var docKey: String
)
