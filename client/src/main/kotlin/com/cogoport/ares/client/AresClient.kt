package com.cogoport.ares.client

import com.cogoport.ares.api.payment.model.OutstandingListRequest
import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import javax.validation.Valid

@Client(id = "ares-service")
interface AresClient {
    @Get("/outstanding/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList?
    @Get("/outstanding/invoice-list")
    suspend fun getInvoiceList(
        @QueryValue("zone") zone: String?,
        @QueryValue("org_id") orgId: String?,
        @QueryValue("page") page: Int? = 1,
        @QueryValue(value = "page_limit") page_limit: Int? = 20
    ): MutableList<CustomerInvoiceResponse>?

    @Get("/outstanding/open-search/add")
    suspend fun addToOpenSearch(
        @QueryValue("zone") zone: String?,
        @QueryValue("org_id") orgId: String
    )
}
