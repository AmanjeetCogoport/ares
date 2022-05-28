package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.model.OutstandingListRequest
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.payment.service.interfaces.PushToClientService
import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/outstanding")
class OutstandingController {
    @Inject
    lateinit var outStandingService: OutStandingService
    @Inject
    lateinit var pushToClientService: PushToClientService

    @Get("/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList? {
        return Response<OutstandingList?>().ok(outStandingService.getOutstandingList(request))
    }
    @Get("/invoice-list")
    suspend fun getInvoiceList(
        @QueryValue("zone") zone: String?,
        @QueryValue("org_id") orgId: String?,
        @QueryValue("page") page: Int? = 1,
        @QueryValue("page_limit") page_limit: Int? = 20
    ): MutableList<CustomerInvoiceResponse>? {
        return Response<MutableList<CustomerInvoiceResponse>?>().ok(
            outStandingService.getInvoiceList(
                zone, orgId,
                page ?: 1, page_limit ?: 10
            )
        )
    }

    @Get("/open-search/add")
    suspend fun addToOpenSearch(
        @QueryValue("zone") zone: String?,
        @QueryValue("org_id") orgId: String
    ) {
        return pushToClientService.pushOutstandingData(zone, orgId)
    }
}
