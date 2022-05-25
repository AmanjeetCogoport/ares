package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class DailyOutstandingResponse(
    @JsonProperty("month") var month: String,
    @JsonProperty("openInvoiceAmount") var openInvoiceAmount: BigDecimal?,
    @JsonProperty("onAccountPayment") var onAccountPayment: BigDecimal?,
    @JsonProperty("outstandings") var outstandings: BigDecimal?,
    @JsonProperty("totalSales") var totalSales: BigDecimal?,
    @JsonProperty("days") var days: Int,
    @JsonProperty("dso") var dsoValue: Double
)
