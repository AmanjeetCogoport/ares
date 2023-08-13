package com.cogoport.ares.api.common.client

import com.cogoport.ares.api.common.models.CogoBankResponse
import com.cogoport.ares.api.common.models.ListOrgStylesRequest
import com.cogoport.ares.api.common.models.TdsDataResponse
import com.cogoport.ares.api.common.models.TdsDataResponseList
import com.cogoport.ares.api.migration.model.GetOrgDetailsRequest
import com.cogoport.ares.api.migration.model.GetOrgDetailsResponse
import com.cogoport.ares.api.migration.model.SerialIdDetailsRequest
import com.cogoport.ares.api.migration.model.SerialIdDetailsResponse
import com.cogoport.ares.model.common.CreateCommunicationRequest
import com.cogoport.ares.model.common.GetOrganizationTradePartyDetailRequest
import com.cogoport.ares.model.common.GetOrganizationTradePartyDetailResponse
import com.cogoport.ares.model.payment.MappingIdDetailRequest
import com.cogoport.ares.model.payment.TradePartyDetailRequest
import com.cogoport.ares.model.payment.TradePartyOrganizationResponse
import com.cogoport.ares.model.payment.ValidateTradePartyRequest
import com.cogoport.ares.model.payment.request.CogoEntitiesRequest
import com.cogoport.ares.model.payment.request.CogoOrganizationRequest
import com.cogoport.ares.model.payment.response.PlatformOrganizationResponse
import com.cogoport.ares.model.sage.SageOrganizationAccountTypeRequest
import com.cogoport.plutus.model.invoice.GetUserRequest
import com.cogoport.plutus.model.invoice.GetUserResponse
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.cogoport.plutus.model.invoice.SageOrganizationResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Headers
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import javax.validation.Valid

@Client(id = "bf-rails")
@Headers(
    Header(name = HttpHeaders.AUTHORIZATION, value = "Bearer: \${cogoport.bearer_token}"),
    Header(name = HttpHeaders.ACCEPT, value = "application/json"),
    Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json"),
    Header(name = HttpHeaders.USER_AGENT, value = "Ares-Cogo-Client"),
)
interface AuthClient {
    @Get("/list_cogo_banks{?request*}")
    suspend fun getCogoBank(request: CogoEntitiesRequest): CogoBankResponse

    @Get("/organization/get_organization_zone_details{?request*}")
    suspend fun getCogoOrganization(request: CogoOrganizationRequest): PlatformOrganizationResponse

    /**
     * Takes trade party mapping id and returns its TDS styles.
     * @param: id
     */
    @Get("/organization/get_organization_trade_party_finance_detail")
    suspend fun getOrgTdsStyles(@QueryValue("id") id: String): TdsDataResponse

    /**
     * Takes trade party mapping id list and returns its TDS styles list.
     * @param: id
     */
    @Post("/organization/list_organization_trade_party_finance_detail_webhook")
    suspend fun listOrgTdsStyles(@Body request: ListOrgStylesRequest): TdsDataResponseList?

    /**
     * Takes trade party detail id and returns TDS styles of corresponding org with mapping type as self.
     */
    @Get("/organization/get_self_organization_trade_party_finance_detail")
    suspend fun getSelfOrgTdsStyles(@QueryValue("id") id: String): TdsDataResponse

    @Get("/get_organization_details_by_sage_org_id{?request*}")
    suspend fun getOrgDetailsBySageOrgId(request: GetOrgDetailsRequest): GetOrgDetailsResponse

    @Get("/organization/get_organization_trade_party_zone_details{?request*}")
    suspend fun getTradePartyDetailInfo(request: TradePartyDetailRequest): TradePartyOrganizationResponse

    @Get("/organization/get_organization_trade_party_zone_info{?request*}")
    suspend fun getTradePartyInfo(request: MappingIdDetailRequest): TradePartyOrganizationResponse

    @Post("/organization/get_organization_trade_party_mappings")
    suspend fun getSerialIdDetails(@Body request: SerialIdDetailsRequest): List<SerialIdDetailsResponse?>?

    @Get("/validate_trade_party{?request*}")
    suspend fun validateTradeParty(request: ValidateTradePartyRequest): Boolean?

    @Post("/list_organization_trade_party_business_finance")
    suspend fun getOrganizationTradePartyDetail(@Body request: GetOrganizationTradePartyDetailRequest): GetOrganizationTradePartyDetailResponse?

    @Post("/user/get_users")
    suspend fun getUsers(@Body request: GetUserRequest): List<GetUserResponse>?

    @Get("/get_sage_organization_details{?request*}")
    suspend fun getSageOrganization(@Valid request: SageOrganizationRequest): SageOrganizationResponse

    @Post("/send_communication_for_finance")
    suspend fun sendCommunication(@Body request: CreateCommunicationRequest): Boolean

    @Get("/get_sage_organization_details{?request*}")
    suspend fun getSageOrganizationAccountType(@Valid request: SageOrganizationAccountTypeRequest): SageOrganizationResponse
}
