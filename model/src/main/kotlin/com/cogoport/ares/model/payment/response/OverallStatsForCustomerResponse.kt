package com.cogoport.ares.model.payment.response


import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
data class OverallStatsForCustomerResponse(
    @JsonProperty("proformaInvoices")
    val proformaInvoices: List<StatsForCustomerResponse?>,
    @JsonProperty("dueForPayment")
    val dueForPayment: List<StatsForCustomerResponse?>,
    @JsonProperty("overdueInvoices")
    val overdueInvoices: List<StatsForCustomerResponse?>,
    @JsonProperty("totalReceivables")
    val totalReceivables: List<StatsForCustomerResponse?>,
    @JsonProperty("onAccountPayment")
    val onAccountPayment: BigDecimal? = 0.toBigDecimal()
)
