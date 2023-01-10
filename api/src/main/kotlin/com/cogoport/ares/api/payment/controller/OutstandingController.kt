package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.service.implementation.Scheduler
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.SupplierOutstandingList
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.math.BigDecimal
import javax.validation.Valid

@Validated
@Controller("/outstanding")
class OutstandingController {
    @Inject
    lateinit var outStandingService: OutStandingService
    @Inject
    lateinit var pushToClientService: OpenSearchService

    @Inject
    lateinit var scheduler: Scheduler

    @Get("/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList? {
        return Response<OutstandingList?>().ok(outStandingService.getOutstandingList(request))
    }
    @Get("/bill-overall{?request*}")
    suspend fun getBillOutstanding(@Valid request: OutstandingListRequest): SupplierOutstandingList? {
        return Response<SupplierOutstandingList?>().ok(outStandingService.getSupplierOutstandingList(request))
    }

    @Get("/by-supplier{?request*}")
    suspend fun getSupplierDetails(@Valid request: SupplierOutstandingRequest): ResponseList<SupplierOutstandingDocument?> {
        return Response<ResponseList<SupplierOutstandingDocument?>>().ok(outStandingService.listSupplierDetails(request))
    }

    @Get("/invoice-list{?request*}")
    suspend fun getInvoiceList(@Valid request: InvoiceListRequest): ListInvoiceResponse? {
        return Response<ListInvoiceResponse?>().ok(outStandingService.getInvoiceList(request))
    }

    @Get("/open-search/add{?request*}")
    suspend fun addToOpenSearch(@Valid request: OpenSearchRequest) {
        return pushToClientService.pushOutstandingData(request)
    }

    @Get("/{orgId}")
    suspend fun getCustomerOutstanding(@PathVariable("orgId") orgId: String): MutableList<CustomerOutstanding?> {
        return Response<MutableList<CustomerOutstanding?>>().ok(outStandingService.getCustomerOutstanding(orgId))
    }

    @Post("/outstanding-days")
    suspend fun getCurrOutstanding(@Body invoiceIds: List<Long>): Long {
        return Response<Long>().ok(outStandingService.getCurrOutstanding(invoiceIds))
    }

    @Post("/customer-outstanding")
    suspend fun getCustomersOutstandingInINR(@Body orgIds: List<String>): MutableMap<String, BigDecimal?> {
        return Response<MutableMap<String, BigDecimal?>>().ok(outStandingService.getCustomersOutstandingInINR(orgIds))
    }

    @Post("/supplier")
    suspend fun createSupplierDetails(@Valid @Body request: SupplierOutstandingDocument): Response<String> {
        outStandingService.createSupplierDetails(request)
        return Response<String>().ok("created", HttpStatus.OK.name)
    }

    @Put("/supplier/{id}")
    suspend fun updateSupplierDetails(@PathVariable("id") id: String) {
        return outStandingService.updateSupplierDetails(id, flag = false, document = null)
    }

    @Put("/supplier-outstanding-migrate")
    suspend fun migrateSupplierOutstanding() {
        return scheduler.updateSupplierOutstandingOnOpenSearch()
    }
}
