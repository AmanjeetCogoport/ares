package com.cogoport.ares.api.common.client

import com.cogoport.ares.api.common.models.CogoBankResponse
import com.cogoport.ares.api.common.models.ExchangeRequest
import com.cogoport.ares.api.common.models.ExchangeResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Headers
import io.micronaut.http.client.annotation.Client

@Client(value = "\${cogoport.exchange_api_url}")
@Headers(
    Header(name = HttpHeaders.ACCEPT, value = "application/json"),
    Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json"),
    Header(name = HttpHeaders.USER_AGENT, value = "Ares-Cogo-Client"),
)
interface ExchangeClient {
    @Get("/rate/exchange_rates{?request*}")
    suspend fun getExchangeRate(request: ExchangeRequest): ExchangeResponse
}
