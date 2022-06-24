package com.cogoport.ares.api.common.client

import com.cogoport.ares.api.common.models.CogoBankResponse
import com.cogoport.ares.model.payment.PlatformOrganizationResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client

@Client(value = "\${cogoport.api_url}")
interface CogoClient {

    @Get("/list_cogo_entities?filters%5Bentity_code%5D={entityCode}")
    @Header(name = "authorizationparameters", value = "business_finance-jobs:allowed")
    @Header(name = "authorization", value = "Bearer: 0cb31ec4-a850-48de-9998-39f93f073f24")
    @Header(name = "authorizationscope", value = "partner")
    suspend fun getCogoBank(entityCode: Int): CogoBankResponse

    @Get("/get_organization_zone_via_org_id?organization_id={id}")
    @Header(name = "authorizationparameters", value = "business_finance-account_receivables:allowed")
    @Header(name = "authorization", value = "Bearer: 1d9e275f-7981-44c2-b01f-390bec756606")
    @Header(name = "authorizationscope", value = "partner")
    suspend fun getCogoOrganization(id: String): PlatformOrganizationResponse
}
