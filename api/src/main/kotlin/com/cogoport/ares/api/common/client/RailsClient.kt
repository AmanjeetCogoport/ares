package com.cogoport.ares.api.common.client

import com.cogoport.ares.model.settlement.CreditPaymentRequest
import com.cogoport.ares.model.settlement.ListCogoEntities
import com.cogoport.ares.model.settlement.ListOrganizationTradePartyDetailsResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Headers
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import java.util.UUID

@Client(value = "\${cogoport.api_url}")
@Headers(
    Header(name = HttpHeaders.AUTHORIZATION, value = "Bearer: \${cogoport.bearer_token_new}"),
    Header(name = "AuthorizationScope", value = "micro_service"),
    Header(name = "AuthorizationScopeId", value = "\${cogoport.auth_scope_id}"),
    Header(name = HttpHeaders.ACCEPT, value = "application/json"),
    Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json"),
    Header(name = HttpHeaders.USER_AGENT, value = "Ares-Cogo-Client"),
)
interface RailsClient {

    @Get("/list_organization_trade_party_details?filters%5Bid%5D={id}")
    suspend fun getListOrganizationTradePartyDetails(id: UUID): ListOrganizationTradePartyDetailsResponse

    @Get("/list_cogo_entities?filters%5Bentity_code%5D={entityCode}")
    suspend fun getCogoEntity(entityCode: String): ListCogoEntities

    @Post("/create_organization_credit_consumption")
    suspend fun sendInvoicePaymentKnockOff(@Body request: com.cogoport.plutus.model.invoice.CreditPaymentRequest): String
}
