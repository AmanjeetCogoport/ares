package com.cogoport.ares.api.common.client

import com.cogoport.ares.api.common.models.CogoBankResponse
import com.cogoport.ares.model.payment.CogoEntitiesRequest
import com.cogoport.ares.model.payment.CogoOrganizationRequest
import com.cogoport.ares.model.payment.PlatformOrganizationResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Headers
import io.micronaut.http.client.annotation.Client

@Client(value = "\${cogoport.api_url}")
@Headers(
    Header(name = HttpHeaders.AUTHORIZATION, value = "Bearer: \${cogoport.bearer_token}"),
    Header(name = HttpHeaders.ACCEPT, value = "application/json"),
    Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json"),
    Header(name = HttpHeaders.USER_AGENT, value = "Ares-Cogo-Client"),
)
interface CogoClient {
    @Get("/list_cogo_banks{?request*}")
    suspend fun getCogoBank(request: CogoEntitiesRequest): CogoBankResponse

    @Get("/get_organization_zone_details{?request*}")
    suspend fun getCogoOrganization(request: CogoOrganizationRequest): PlatformOrganizationResponse
}
