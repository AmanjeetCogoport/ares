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
data class OverallStatsForKamResponse(
    @JsonProperty("proformaInvoices")
    val proformaInvoices: List<StatsForKamResponse?>,
    @JsonProperty("dueForPayment")
    val dueForPayment: List<StatsForKamResponse?>,
    @JsonProperty("overdueInvoices")
    val overdueInvoices: List<StatsForKamResponse?>,
    @JsonProperty("totalReceivables")
    val totalReceivables: List<StatsForKamResponse?>
)
