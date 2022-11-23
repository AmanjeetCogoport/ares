package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class ExchangeRateForPeriodRequest(
    val currencyList: List<String>,
    val dashboardCurrency: String
)
