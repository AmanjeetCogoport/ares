package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OrgPayableRequest
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.request.CollectionRequest
import com.cogoport.ares.model.payment.request.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.OverallStatsRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.ReceivableRequest
import com.cogoport.ares.model.payment.response.*
import io.micronaut.http.annotation.*
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/dashboard")
class DashboardController {
    @Inject
    lateinit var dashboardService: DashboardService
    @Inject
    lateinit var pushToClientService: OpenSearchService
    @Get("/overall-stats{?request*}")
    suspend fun getOverallStats(@Valid request: OverallStatsRequest): OverallStatsResponse? {
        return Response<OverallStatsResponse?>().ok(dashboardService.getOverallStats(request))
    }

    @Get("/daily-sales-outstanding{?request*}")
    suspend fun getDailySalesOutstanding(@Valid request: DsoRequest): DailySalesOutstanding? {
        return Response<DailySalesOutstanding?>().ok(dashboardService.getDailySalesOutstanding(request))
    }

    @Get("/collection-trend{?request*}")
    suspend fun getCollectionTrend(@Valid request: CollectionRequest): CollectionResponse? {
        return Response<CollectionResponse?>().ok(dashboardService.getCollectionTrend(request))
    }

    @Get("/monthly-outstanding{?request*}")
    suspend fun getMonthlyOutstanding(@Valid request: MonthlyOutstandingRequest): MonthlyOutstanding? {
        return Response<MonthlyOutstanding?>().ok(dashboardService.getMonthlyOutstanding(request))
    }

    @Get("/quarterly-outstanding{?request*}")
    suspend fun getQuarterlyOutstanding(@Valid request: QuarterlyOutstandingRequest): QuarterlyOutstanding? {
        return Response<QuarterlyOutstanding?>().ok(dashboardService.getQuarterlyOutstanding(request))
    }

    @Get("outstanding-by-age{?request*}")
    suspend fun getOutStandingByAge(@Valid request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse>? {
        return Response<List<OverallAgeingStatsResponse>?>().ok(dashboardService.getOutStandingByAge(request))
    }

    @Get("/receivables-by-age{?request*}")
    suspend fun getReceivablesByAge(@Valid request: ReceivableRequest): ReceivableAgeingResponse {
        return Response<ReceivableAgeingResponse>().ok(dashboardService.getReceivableByAge(request))
    }

    @Get("/org-collection{?request*}")
    suspend fun getOrgCollection(@Valid request: OrganizationReceivablesRequest): List<OutstandingResponse> {
        return Response<List<OutstandingResponse>>().ok(dashboardService.getOrgCollection(request))
    }

    @Get("/org-payables-stats{?request*}")
    suspend fun getOrgPayables(@Valid request: OrgPayableRequest): OrgPayableResponse {
        return Response<OrgPayableResponse>().ok(dashboardService.getOrgPayables(request))
    }

    @Get("/overallStatsForKam")
    suspend fun getOverallStatsForKam(@QueryValue("docValue") docValue: List<String>): OverallStatsForKamResponse {
        return Response<OverallStatsForKamResponse>().ok(dashboardService.getOverallStatsForKam(docValue))
    }

    @Get("/overallStatsForCustomers")
    suspend fun getOverallStatsForCustomers(
        @PathVariable("proformaNumbers") listOfConsolidatedAddresses: List<ConsolidatedAddresses>,
        @PathVariable("proformaNumbers") proformaNumbers: List<String>,
        @PathVariable("pageNumber") pageNumber: Int?,
        @PathVariable("pageLimit") pageLimit: Int?
    ): List<OverallStatsForCustomerResponse> {
        return Response<List<OverallStatsForCustomerResponse>>().ok(dashboardService.getOverallStatsForCustomers(listOfConsolidatedAddresses,proformaNumbers,pageNumber,pageLimit))
    }

    /** To be Deleted */

    @Get("/open-search/add{?request*}")
    suspend fun addToOpenSearch(@Valid request: OpenSearchRequest) { return pushToClientService.pushDashboardData(request) }

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }
}
