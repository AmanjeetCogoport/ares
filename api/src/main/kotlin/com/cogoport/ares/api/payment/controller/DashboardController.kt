package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.model.CollectionRequest
import com.cogoport.ares.api.payment.model.DailyOutstandingRequest
import com.cogoport.ares.api.payment.model.OverallStatsRequest
import com.cogoport.ares.api.payment.model.MonthlyOutstandingRequest
import com.cogoport.ares.api.payment.model.QuarterlyOutstandingRequest
import com.cogoport.ares.api.payment.model.SalesTrendRequest
import com.cogoport.ares.api.payment.model.OutstandingAgeingRequest
import com.cogoport.ares.api.payment.model.ReceivableRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.PushToClientService
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.SalesTrendResponse
import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OverallStatsResponse
import com.cogoport.brahma.opensearch.Client
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid
@Validated
@Controller("/dashboard")
class DashboardController {
    @Inject
    lateinit var dashboardService: DashboardService
    @Inject
    lateinit var pushToClientService: PushToClientService
    @Get("/overall-stats{?request*}")
    suspend fun getOverallStats(@Valid request: OverallStatsRequest): OverallStatsResponse? {
        return Response<OverallStatsResponse?>().ok(dashboardService.getOverallStats(request.zone, request.role))
    }

    @Get("/daily-sales-outstanding{?request*}")
    suspend fun getDailySalesOutstandingWidget(@Valid request: DailyOutstandingRequest): DailySalesOutstanding? {
        return Response<DailySalesOutstanding?>().ok(dashboardService.getDailySalesOutstanding(request.zone, request.role, request.quarter, request.year))
    }

    @Get("/collection-trend{?request*}")
    suspend fun getCollectionTrend(@Valid request: CollectionRequest): CollectionResponse? {
        return Response<CollectionResponse?>().ok(dashboardService.getCollectionTrend(request.zone, request.role, request.quarter, request.year))
    }

    @Get("/monthly-outstanding{?request*}")
    suspend fun getMonthlyOutstanding(@Valid request: MonthlyOutstandingRequest): MonthlyOutstanding? {
        return Response<MonthlyOutstanding?>().ok(dashboardService.getMonthlyOutstanding(request.zone, request.role))
    }

    @Get("/quarterly-outstanding{?request*}")
    suspend fun getQuarterlyOutstanding(@Valid request: QuarterlyOutstandingRequest): QuarterlyOutstanding? {
        return Response<QuarterlyOutstanding?>().ok(dashboardService.getQuarterlyOutstanding(request.zone, request.role))
    }
    @Get("/sales-trend{?request*}")
    suspend fun getSalesTrend(@Valid request: SalesTrendRequest): SalesTrendResponse? {
        return Response<SalesTrendResponse?>().ok(dashboardService.getSalesTrend(request.zone, request.role))
    }

    @Get("outstanding-by-age{?request*}")
    suspend fun getOutStandingByAge(@Valid request: OutstandingAgeingRequest): MutableList<OverallAgeingStatsResponse>? {
        return Response<MutableList<OverallAgeingStatsResponse>?>().ok(dashboardService.getOutStandingByAge(request.zone, request.role))
    }

    @Get("/receivables-by-age{?request*}")
    suspend fun getReceivablesByAge(@Valid request: ReceivableRequest): ReceivableAgeingResponse {
        return Response<ReceivableAgeingResponse>().ok(dashboardService.getReceivableByAge(request.zone, request.role))
    }

    /** To be Deleted */
//    @Inject
//    lateinit var accountUtilizationRepository: AccountUtilizationRepository
//    @Get("/push-plutus")
//    suspend fun pushPlutus(){
//        val invoiceList = accountUtilizationRepository.getInvoices()
//        for (invoice in invoiceList){
//            Client.updateDocument("sale_invoice_index",invoice.id.toString(),invoice)
//        }
//    }
    @Get("/open-search/add")
    suspend fun addToOpenSearch(@QueryValue("zone") zone: String?, @QueryValue("date") date: String, @QueryValue("quarter") quarter: Int?) { return pushToClientService.pushDashboardData(zone, date, quarter) }

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }
}
