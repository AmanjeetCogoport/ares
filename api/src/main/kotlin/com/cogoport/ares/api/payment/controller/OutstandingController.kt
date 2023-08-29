package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.service.implementation.Scheduler
import com.cogoport.ares.api.payment.entity.EntityLevelStats
import com.cogoport.ares.api.payment.entity.EntityWiseOutstandingBucket
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentRequest
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.model.response.TopServiceProviders
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.common.TradePartyOutstandingReq
import com.cogoport.ares.model.common.TradePartyOutstandingRes
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.SupplierOutstandingList
import com.cogoport.ares.model.payment.request.AccPayablesOfOrgReq
import com.cogoport.ares.model.payment.request.CustomerMonthlyPaymentRequest
import com.cogoport.ares.model.payment.request.CustomerOutstandingRequest
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequestV2
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.AccPayablesOfOrgRes
import com.cogoport.ares.model.payment.response.CustomerMonthlyPayment
import com.cogoport.ares.model.payment.response.CustomerOutstandingDocumentResponse
import com.cogoport.ares.model.payment.response.PayblesInfoRes
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocumentV2
import com.cogoport.brahma.authentication.Auth
import com.cogoport.brahma.authentication.AuthResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
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

    @Inject
    lateinit var util: Util
    @Get("/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList? {
        return Response<OutstandingList?>().ok(outStandingService.getOutstandingList(request))
    }
    @Get("/bill-overall{?request*}")
    suspend fun getBillOutstanding(@Valid request: OutstandingListRequest): SupplierOutstandingList? {
        return Response<SupplierOutstandingList?>().ok(outStandingService.getSupplierOutstandingList(request))
    }

    @Auth
    @Get("/by-supplier{?request*}")
    suspend fun getSupplierDetails(@Valid request: SupplierOutstandingRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<SupplierOutstandingDocument?> {
        request.flag = util.getCogoEntityCode(user?.filters?.get("partner_id")) ?: request.flag
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

    @Put("/supplier")
    suspend fun updateSupplierDetails(@Valid @Body request: UpdateSupplierOutstandingRequest) {
        return outStandingService.updateSupplierDetails(request.orgId.toString(), flag = false, document = null)
    }

    @Put("/supplier-outstanding-migrate")
    suspend fun migrateSupplierOutstanding() {
        return scheduler.updateSupplierOutstandingOnOpenSearch()
    }

    @Post("/customer")
    suspend fun createCustomerDetails(@Valid @Body request: CustomerOutstandingDocumentResponse): Response<String> {
        outStandingService.createCustomerDetails(request)
        return Response<String>().ok("created", HttpStatus.OK.name)
    }

    @Put("/customer")
    suspend fun updateCustomerDetails(request: UpdateSupplierOutstandingRequest) {
        return outStandingService.updateCustomerDetails(request.orgId.toString(), flag = false, document = null)
    }

    @Auth
    @Get("/by-customer{?request*}")
    suspend fun getCustomerDetails(@Valid request: CustomerOutstandingRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<CustomerOutstandingDocumentResponse?> {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return Response<ResponseList<CustomerOutstandingDocumentResponse?>>().ok(outStandingService.listCustomerDetails(request))
    }

    @Put("/customer-outstanding-migrate")
    suspend fun migrateCustomerOutstanding(): HttpResponse<Map<String, String>> {
        scheduler.updateCustomerOutstandingOnOpenSearch()
        return HttpResponse.ok(mapOf("status" to "ok"))
    }

    @Auth
    @Get("/customer-payment{?request*}")
    suspend fun getCustomerOutstandingPaymentDetails(@Valid request: CustomerOutstandingPaymentRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<CustomerOutstandingPaymentResponse?> {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return Response<ResponseList<CustomerOutstandingPaymentResponse?>>().ok(outStandingService.getCustomerOutstandingPaymentDetails(request))
    }
    @Auth
    @Get("/paybles-info")
    suspend fun getPayblesInfo(@QueryValue entity: Int?, user: AuthResponse?, httpRequest: HttpRequest<*>): PayblesInfoRes {
        val entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: entity
        return outStandingService.getPayablesInfo(entityCode)
    }
    @Auth
    @Get("/top-ten-service-providers{?request*}")
    suspend fun getTopTenServiceProviders(@Valid request: SupplierOutstandingRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): TopServiceProviders {
        request.flag = util.getCogoEntityCode(user?.filters?.get("partner_id")) ?: request.flag
        return outStandingService.getTopTenServiceProviders(request)
    }

    @Get("/{orgId}")
    suspend fun getCustomerOutstanding(@PathVariable("orgId") orgId: String): MutableList<CustomerOutstanding?> {
        return Response<MutableList<CustomerOutstanding?>>().ok(outStandingService.getCustomerOutstanding(orgId))
    }

    @Get("/account-payables-for-org{?request*}")
    suspend fun getApOfOrganization(@Valid request: AccPayablesOfOrgReq): List<AccPayablesOfOrgRes> {
        return Response<List<AccPayablesOfOrgRes>>().ok(outStandingService.getPayableOfOrganization(request))
    }

    @Get("/customer-monthly-payment{?request*}")
    suspend fun getCustomerMonthlyPayment(@Valid request: CustomerMonthlyPaymentRequest): CustomerMonthlyPayment {
        return Response<CustomerMonthlyPayment>().ok(outStandingService.getCustomerMonthlyPayment(request))
    }

    @Get("/trade-party-outstanding{?request*}")
    suspend fun getTradePartyOutstanding(@Valid request: TradePartyOutstandingReq): List<TradePartyOutstandingRes>? {
        return outStandingService.getTradePartyOutstanding(request)
    }

    @Get("/ledger-summary")
    suspend fun createLedgerSummary() {
        return outStandingService.createLedgerSummary()
    }

    @Auth
    @Get("/overall-customer-outstanding")
    suspend fun getOverallCustomerOutstanding(
        @QueryValue("entityCode") entityCode: Int? = 301,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): HashMap<String, EntityWiseOutstandingBucket>? {
        val updatedEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: entityCode
        return outStandingService.getOverallCustomerOutstanding(updatedEntityCode!!)
    }

    @Post("/supplier-v2")
    suspend fun createSupplierDetailsV2(): Response<String> {
        outStandingService.createSupplierDetailsV2()
        return Response<String>().ok("created", HttpStatus.OK.name)
    }

    @Auth
    @Get("/by-supplier-v2{?request*}")
    suspend fun listSupplierDetailsV2(@Valid request: SupplierOutstandingRequestV2, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<SupplierOutstandingDocumentV2?> {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return Response<ResponseList<SupplierOutstandingDocumentV2?>>().ok(outStandingService.listSupplierDetailsV2(request))
    }

    @Auth
    @Get("/entity-level-stats")
    suspend fun getEntityLevelStats(@QueryValue("entityCode") entityCode: Int, user: AuthResponse?, httpRequest: HttpRequest<*>): List<EntityLevelStats> {
        val updatedEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: entityCode
        return outStandingService.getEntityLevelStats(updatedEntityCode)
    }
}
