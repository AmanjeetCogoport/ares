package com.cogoport.ares.api.common.client

import com.cogoport.ares.api.common.models.CogoBankResponse
import com.cogoport.ares.api.common.models.TdsDataResponse
import com.cogoport.ares.model.payment.MappingIdDetailRequest
import com.cogoport.ares.model.payment.TradePartyDetailRequest
import com.cogoport.ares.model.payment.TradePartyOrganizationResponse
import com.cogoport.ares.model.payment.request.CogoEntitiesRequest
import com.cogoport.ares.model.payment.request.CogoOrganizationRequest
import com.cogoport.ares.model.payment.response.PlatformOrganizationResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Headers
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client(value = "\${cogoport.api_url}")
@Headers(
    Header(name = HttpHeaders.AUTHORIZATION, value = "Bearer: \${cogoport.bearer_token}"),
    Header(name = HttpHeaders.ACCEPT, value = "application/json"),
    Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json"),
    Header(name = HttpHeaders.USER_AGENT, value = "Ares-Cogo-Client"),
)
interface AuthClient {
    @Get("/list_cogo_banks{?request*}")
    suspend fun getCogoBank(request: CogoEntitiesRequest): CogoBankResponse

    @Get("/get_organization_zone_details{?request*}")
    suspend fun getCogoOrganization(request: CogoOrganizationRequest): PlatformOrganizationResponse

    @Get("/get_organization_trade_party_finance_detail")
    suspend fun getOrgTdsStyles(@QueryValue("id") id: String): TdsDataResponse

    @Get("/get_organization_trade_party_zone_details{?request*}")
    suspend fun getTradePartyDetailInfo(request: TradePartyDetailRequest): TradePartyOrganizationResponse

    @Get("/get_organization_trade_party_zone_info{?request*}")
    suspend fun getTradePartyInfo(request: MappingIdDetailRequest): TradePartyOrganizationResponse
}
