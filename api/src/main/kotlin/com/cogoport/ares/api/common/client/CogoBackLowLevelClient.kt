package com.cogoport.ares.api.common.client

import com.cogoport.ares.model.common.ListOrganizationPaymentModeReq
import com.cogoport.ares.model.common.ListOrganizationPaymentModesRes
import com.cogoport.ares.model.payment.TradePartyOutstandingList
import com.cogoport.ares.model.settlement.ListOrganizationTradePartyDetailsResponse
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
    fun getTradePartyDetailsByRegistrationNumber(regNums: MutableList<String>, endpoint: String): ListOrganizationTradePartyDetailsResponse? {
        val uri: URI = URI.create("$apiUrl/$endpoint?filters[registration_number][]=${regNums.joinToString("&filters[registration_number][]=")}&page_limit=100&page=1")
        val req: HttpRequest<*> = HttpRequest.GET<Any>(uri)
            .header(HttpHeaders.USER_AGENT, "Ares-Cogo-Client")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.AUTHORIZATION, "Bearer: $token")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("AuthorizationScope", "micro_service")
            .header("AuthorizationScopeId", authorizationScopeId)

        val res = Flux.from(httpClient.exchange(req, ListOrganizationTradePartyDetailsResponse::class.java)).blockFirst()?.body()
        return res
    }

    fun listOrganizationPaymentModes(listOrganizationPaymentModeReq: ListOrganizationPaymentModeReq): ListOrganizationPaymentModesRes? {
        val uri: URI = URI.create("$apiUrl/organization/list_organization_payment_modes?filters[organization_trade_party_detail_id]=${listOrganizationPaymentModeReq.tradePartyDetailId}&filters[trade_party_type]=${listOrganizationPaymentModeReq.tradePartyType}")
        val req: HttpRequest<*> = HttpRequest.GET<Any>(uri)
            .header(HttpHeaders.USER_AGENT, "Ares-Cogo-Client")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.AUTHORIZATION, "Bearer: $token")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("AuthorizationScope", "micro_service")
            .header("AuthorizationScopeId", authorizationScopeId)

        val res = Flux.from(httpClient.exchange(req, ListOrganizationPaymentModesRes::class.java)).blockFirst()?.body()
        return res
    }
}
