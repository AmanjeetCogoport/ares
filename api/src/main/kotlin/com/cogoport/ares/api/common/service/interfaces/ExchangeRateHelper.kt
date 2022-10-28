package com.cogoport.ares.api.common.service.interfaces

import java.math.BigDecimal

interface ExchangeRateHelper {
    suspend fun getExchangeRateForPeriod(request: List<String>, dashboardCurrency: String): HashMap<String, BigDecimal>
}