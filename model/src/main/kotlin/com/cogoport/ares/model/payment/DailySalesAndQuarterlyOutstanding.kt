package com.cogoport.ares.model.payment

import com.cogoport.ares.model.payment.response.DsoResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class DailySalesAndQuarterlyOutstanding(
    var QUARTERLY: List<OutstandingResponse>? = null,
    var DAILY: List<DsoResponse>? = null,
    @JsonProperty("dashboardCurrency")
    val dashboardCurrency: String? = "INR"
)
