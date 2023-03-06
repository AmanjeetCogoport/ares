package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class OverallStats(
    @JsonProperty("totalOutstandingAmount")
    var totalOutstandingAmount: BigDecimal?,
    @JsonProperty("openInvoicesCount")
    var openInvoicesCount: Int?,
    @JsonProperty("openInvoicesAmount")
    var openInvoicesAmount: BigDecimal?,
    @JsonProperty("dashboardCurrency")
    var dashboardCurrency: String,
    @JsonProperty("customersCount")
    var customersCount: Int? = 0,
    @JsonProperty("openInvoiceAmountForPast7DaysPercentage")
    var openInvoiceAmountForPast7DaysPercentage: Long? = 0,
    @JsonProperty("onAccountAmount")
    var onAccountAmount: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("onAccountAmountForPastSevenDaysPercentage")
    var onAccountAmountForPastSevenDaysPercentage: Long? = 0
)
