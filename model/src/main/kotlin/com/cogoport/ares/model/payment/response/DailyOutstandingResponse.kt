package com.cogoport.ares.model.payment.response

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
data class DailyOutstandingResponse(
    @JsonProperty("month") var month: Int,
    @JsonProperty("openInvoiceAmount") var openInvoiceAmount: BigDecimal?,
    @JsonProperty("onAccountPayment") var onAccountPayment: BigDecimal?,
    @JsonProperty("outstandings") var outstandings: BigDecimal?,
    @JsonProperty("totalSales") var totalSales: BigDecimal?,
    @JsonProperty("days") var days: Int,
    @JsonProperty("value") var value: BigDecimal,
    @JsonProperty("serviceType") var serviceType: String?,
    @JsonProperty("currencyType") var currencyType: String?
)
