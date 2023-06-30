package com.cogoport.ares.api.gateway

import com.cogoport.ares.api.common.models.ExchangeRequest
import com.cogoport.ares.api.common.models.ExchangeRequestPeriod
import com.cogoport.ares.api.common.models.ExchangeResponse
import com.cogoport.ares.api.common.models.ExchangeResponseForPeriod
import com.cogoport.ares.model.common.ExchangeRateRequest
import com.cogoport.ares.model.common.ExchangeRateResponseByDate
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Headers
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@Client(value = "\${cogoport.exchange_api.url}")
@Headers(
    Header(name = HttpHeaders.ACCEPT, value = "application/json"),
    Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json"),
    Header(name = HttpHeaders.USER_AGENT, value = "Ares-Cogo-Client"),
    Header(name = "auth-token", value = "\${cogoport.exchange_api.auth_token}")
)
interface ExchangeClient {
    @Get("/exchange_rates{?request*}")
    suspend fun getExchangeRate(request: ExchangeRequest): ExchangeResponse

    @Post("/avg-rate")
    suspend fun getExchangeRateForPeriod(@Body request: HashMap<String, List<ExchangeRequestPeriod>>): ArrayList<ExchangeResponseForPeriod>

    @Get("/exchange-rate")
    suspend fun getExchangeRates(request: ExchangeRateRequest): MutableList<ExchangeRateResponseByDate>
}
