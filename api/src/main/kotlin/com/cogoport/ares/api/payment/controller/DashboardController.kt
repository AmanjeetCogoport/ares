package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.PushToClientService
import com.cogoport.ares.model.payment.*
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/dashboard")
class DashboardController {
    @Inject
    lateinit var dashboardService: DashboardService
    @Inject
    lateinit var pushToClientService: PushToClientService

    @Get("/overall-stats")
    suspend fun getOverallStats(
        @QueryValue(AresConstants.ROLE) zone: String?,
        @QueryValue(AresConstants.ZONE) role: String?
    ): OverallStatsResponse? {
        return Response<OverallStatsResponse?>().ok(dashboardService.getOverallStats(zone, role))
    }

    @Get("/daily-sales-outstanding-widget")
    suspend fun getDailySalesOutstandingWidget(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?,
        @QueryValue(AresConstants.QUARTER) quarter: String
    ): DailySalesOutstanding? {
        return Response<DailySalesOutstanding?>().ok(dashboardService.getDailySalesOutstanding(zone, role, quarter))
    }

    @Get("/collection-trend")
    suspend fun getCollectionTrend(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?,
        @QueryValue(AresConstants.QUARTER) quarter: String
    ): CollectionTrendResponse? {
        return Response<CollectionTrendResponse?>().ok(dashboardService.getCollectionTrend(zone, role, quarter))
    }

    @Get("/monthly-outstanding")
    suspend fun getMonthlyOutstanding(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?
    ): MonthlyOutstanding? {
        return Response<MonthlyOutstanding?>().ok(dashboardService.getMonthlyOutstanding(zone, role))
    }

    @Get("/quarterly-outstanding")
    suspend fun getQuarterlyOutstanding(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?
    ): QuarterlyOutstanding? {
        return Response<QuarterlyOutstanding?>().ok(dashboardService.getQuarterlyOutstanding(zone, role))
    }

    @Get("/open-search/add")
    suspend fun addToOpenSearch(@QueryValue("zone") zone: String?) { return pushToClientService.pushDataToOpenSearch(zone) }

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }

    @Get("/sales-trend")
    suspend fun getSalesTrend(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?
    ): SalesTrendResponse? {
        return Response<SalesTrendResponse?>().ok(dashboardService.getSalesTrend(zone, role))
    }

    @Get("outstanding-by-age")
    suspend fun getOutStandingByAge(@QueryValue("zone") zone: String?): MutableList<OverallAgeingStatsResponse>? {
        return Response<MutableList<OverallAgeingStatsResponse>?>().ok(dashboardService.getOutStandingByAge(zone))
    }

    @Get("/receivables-by-age")
    suspend fun getReceivablesByAge(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): ReceivableAgeingResponse {
        return Response<ReceivableAgeingResponse>().ok(dashboardService.getReceivableByAge(zone, role))
    }
}
