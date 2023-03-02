package com.cogoport.ares.api.common.client

import com.cogoport.ares.model.payment.TradePartyOutstandingList
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.uri.UriBuilder
import jakarta.inject.Singleton
import reactor.core.publisher.Flux
import java.net.URI

@Singleton
class CogoBackLowLevelClient(private val httpClient: HttpClient) {

    @Value("\${cogoport.api_url}")
    lateinit var apiUrl: String
    @Value("\${cogoport.bearer_token_new}")
    lateinit var token: String
    @Value("\${cogoport.auth_scope_id}")
    lateinit var authorizationScopeId: String

    fun getTradePartyOutstanding(id: String, endpoint: String): TradePartyOutstandingList? {
        val uri: URI = UriBuilder.of(apiUrl)
            .path(endpoint)
            .queryParam("org_ids[]", id)
            .build()
        val req: HttpRequest<*> = HttpRequest.GET<Any>(uri)
            .header(HttpHeaders.USER_AGENT, "Ares-Cogo-Client")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.AUTHORIZATION, "Bearer: $token")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("AuthorizationScope", "micro_service")
            .header("AuthorizationScopeId", authorizationScopeId)

        val res = Flux.from(httpClient.exchange(req, TradePartyOutstandingList::class.java)).blockFirst()?.body()
        return res
    }
}
