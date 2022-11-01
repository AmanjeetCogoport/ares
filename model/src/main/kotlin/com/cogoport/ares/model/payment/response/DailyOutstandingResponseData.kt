package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class DailyOutstandingResponseData(
    @JsonProperty("month") var month: Int,
    @JsonProperty("days") var days: Int,
    @JsonProperty("openInvoiceAmount") var openInvoiceAmount: BigDecimal?,
    @JsonProperty("onAccountPayment") var onAccountPayment: BigDecimal?,
    @JsonProperty("outstandings") var outstandings: BigDecimal?,
    @JsonProperty("totalSales") var totalSales: BigDecimal?,
    @JsonProperty("value") var value: BigDecimal,
    @JsonProperty("dashboardCurrency") var dashboardCurrency: String?
)
