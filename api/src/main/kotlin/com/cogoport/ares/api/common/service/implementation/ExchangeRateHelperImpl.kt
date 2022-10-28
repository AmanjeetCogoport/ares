package com.cogoport.ares.api.common.service.implementation

import com.cogoport.ares.api.common.models.ExchangeRequestPeriod
import com.cogoport.ares.api.common.service.interfaces.ExchangeRateHelper
import com.cogoport.ares.api.gateway.ExchangeClient
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter

@Singleton
class ExchangeRateHelperImpl: ExchangeRateHelper {
    @Inject
    lateinit var exchangeClient: ExchangeClient

    override suspend fun getExchangeRateForPeriod(
        request: List<String>,
        dashboardCurrency: String
    ): HashMap<String, BigDecimal> {
        val endDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()
        val startDate =
            LocalDateTime.now().minus(Period.ofDays(30)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()

        val arrayListOfExchangeRateRequest: List<ExchangeRequestPeriod> = request.map { it ->
            ExchangeRequestPeriod(
                fromCurrency = it,
                toCurrency = dashboardCurrency,
                startDate,
                endDate
            )
        }

        val hashMapForExchangeRequest = HashMap<String, List<ExchangeRequestPeriod>>()
        hashMapForExchangeRequest["rate_request_body"] = arrayListOfExchangeRateRequest

        val response = exchangeClient.getExchangeRateForPeriod(hashMapForExchangeRequest)

        val responseData = HashMap<String, BigDecimal>()

        response.map {
            responseData.put(it.fromCurrencyType, it.exchangeRate)
        }
        return responseData
    }
}