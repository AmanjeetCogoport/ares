package com.cogoport.ares.api.common.client

import com.cogoport.ares.model.common.GetPartnerRequest
import com.cogoport.ares.model.settlement.ListCogoEntities
import com.cogoport.ares.model.settlement.ListOrganizationTradePartyDetailsResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Headers
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import java.util.UUID

@Client(id = "bf-rails")
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
    @Get("/get_partner{?request*}")
    suspend fun getPartnerDetails(@QueryValue request: GetPartnerRequest): Any?

    @Get("/list_communication_templates?filters%5Bid%5D={id}")
    suspend fun listCommunicationTemplate(id: UUID): ListOrganizationTradePartyDetailsResponse

    @Get("/list_partners?filters%5Btwin_importer_exporter_id%5D={organizationId}")
    suspend fun listPartners(organizationId: UUID): ListOrganizationTradePartyDetailsResponse

    @Get("/get_channel_partner_users?partner_id={partnerId}&account_types[]=importer_exporter")
    suspend fun getCpUsers(partnerId: UUID): ListOrganizationTradePartyDetailsResponse

    @Get("/list_organization_users?filters%5Borganization_id%5D={orgId}&status=active")
    suspend fun listOrgUsers(orgId: UUID): ListOrganizationTradePartyDetailsResponse

    @Get("/list_organization_trade_parties?filters%5Borganization_trade_party_detail_id%5D={tradePartyDetailId}&billing_addresses_data_required=true")
    suspend fun listTradeParties(tradePartyDetailId: UUID): ListOrganizationTradePartyDetailsResponse

    @Get("/list_communications?filters%5Brecipient%5D={email}&filters%5Bservice%5D=dunning_cycle&third_party_response_required=true")
    suspend fun listCommunication(email: String): ListOrganizationTradePartyDetailsResponse
}
