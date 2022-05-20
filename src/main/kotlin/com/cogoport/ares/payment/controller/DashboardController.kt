package com.cogoport.ares.payment.controller

import com.cogoport.ares.common.AresConstants
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.payment.model.AgeingBucket
import com.cogoport.ares.payment.model.CollectionTrend
import com.cogoport.ares.payment.model.DailySalesOutstanding
import com.cogoport.ares.payment.model.MonthlyOutstanding
import com.cogoport.ares.payment.model.OverallStats
import com.cogoport.ares.payment.model.QuarterlyOutstanding
import com.cogoport.ares.payment.model.ReceivableAgeingResponse
import com.cogoport.ares.payment.model.SalesTrendResponse
import com.cogoport.ares.payment.service.interfaces.DashboardService
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

    @Get("/overall-stats")
    suspend fun getOverallStats(
        @QueryValue(AresConstants.ROLE) zone: String?,
        @QueryValue(AresConstants.ZONE) role: String?
    ): OverallStats? {
        return Response<OverallStats?>().ok(dashboardService.getOverallStats(zone, role))
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
    ): CollectionTrend? {
        return Response<CollectionTrend?>().ok(dashboardService.getCollectionTrend(zone, role, quarter))
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
    suspend fun getOutstandingByAge() { return dashboardService.pushDataToOpenSearch() }

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
    suspend fun getOutStandingByAge(): List<AgeingBucket>? {
        return Response<List<AgeingBucket>?>().ok(dashboardService.getOutStandingByAge())
    }

    @Get("/receivables-by-age")
    suspend fun getReceivablesByAge(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): ReceivableAgeingResponse {
        return Response<ReceivableAgeingResponse>().ok(dashboardService.getReceivableByAge(zone, role))
    }
}
