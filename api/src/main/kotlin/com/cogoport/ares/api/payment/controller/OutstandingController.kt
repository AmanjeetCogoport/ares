package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.model.payment.InvoiceListRequest
import com.cogoport.ares.model.payment.OutstandingListRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.model.payment.CustomerInvoiceList
import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.CustomerOutstanding
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/outstanding")
class OutstandingController {
    @Inject
    lateinit var outStandingService: OutStandingService
    @Inject
    lateinit var pushToClientService: OpenSearchService

    @Get("/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList? {
        return Response<OutstandingList?>().ok(outStandingService.getOutstandingList(request))
    }

    @Get("/invoice-list{?request*}")
    suspend fun getInvoiceList(@Valid request: InvoiceListRequest): CustomerInvoiceList? {
        return Response<CustomerInvoiceList?>().ok(outStandingService.getInvoiceList(request))
    }

    @Get("/open-search/add{?request*}")
    suspend fun addToOpenSearch(@Valid request: OpenSearchRequest) {
        return pushToClientService.pushOutstandingData(request)
    }

    @Get("/{orgId}")
    suspend fun getCustomerOutstanding(@PathVariable("orgId") orgId: String): MutableList<CustomerOutstanding?> {
        return Response<MutableList<CustomerOutstanding?>>().ok(outStandingService.getCustomerOutstanding(orgId))
    }
}
