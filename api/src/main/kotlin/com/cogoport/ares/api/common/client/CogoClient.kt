package com.cogoport.ares.api.common.client

import com.cogoport.ares.api.common.models.CogoBankResponse
import com.cogoport.ares.model.payment.CogoEntitiesRequest
import com.cogoport.ares.model.payment.CogoOrganizationRequest
import com.cogoport.ares.model.payment.PlatformOrganizationResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client

@Client(value = "\${cogoport.api_url}")
@Header(name = "authorization", value = "\${cogoport.bearer_token}")
interface CogoClient {

    @Get("/list_cogo_banks{?request*}")
    suspend fun getCogoBank(request: CogoEntitiesRequest): CogoBankResponse

    @Get("/get_organization_zone_details{?request*}")
    suspend fun getCogoOrganization(request: CogoOrganizationRequest): PlatformOrganizationResponse
}
