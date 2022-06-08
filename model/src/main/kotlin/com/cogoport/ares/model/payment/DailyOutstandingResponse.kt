package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DailyOutstandingResponse(
    @JsonProperty("month") var month: Int,
    @JsonProperty("openInvoiceAmount") var openInvoiceAmount: BigDecimal?,
    @JsonProperty("onAccountPayment") var onAccountPayment: BigDecimal?,
    @JsonProperty("outstandings") var outstandings: BigDecimal?,
    @JsonProperty("totalSales") var totalSales: BigDecimal?,
    @JsonProperty("days") var days: Int,
    @JsonProperty("value") var value: Float
)
