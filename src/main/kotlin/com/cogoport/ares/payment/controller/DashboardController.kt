package com.cogoport.ares.payment.controller

import com.cogoport.ares.common.models.Response
import com.cogoport.ares.payment.model.CollectionTrend
import com.cogoport.ares.payment.model.MonthlyOutstanding
import com.cogoport.ares.payment.model.OverallOutstandingStats
import com.cogoport.ares.payment.model.SalesTrend
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

    @Get("/outstanding-stats")
    suspend fun getOutstandingStats(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): OverallOutstandingStats? {
        return Response<OverallOutstandingStats?>().ok(dashboardService.getOverallOutstanding(zone, role))
    }

    @Get("/collection-trend")
    suspend fun getCollectionTrend(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?,
        @QueryValue("quarter") quarter: String
    ): CollectionTrend? {
        return Response<CollectionTrend?>().ok(dashboardService.getCollectionTrend(zone, role, quarter))
    }

    @Get("/monthly-outstanding")
    suspend fun getMonthlyOutstanding(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): MonthlyOutstanding? {
        return Response<MonthlyOutstanding?>().ok(dashboardService.getMonthlyOutstanding(zone, role))
    }

    @Get("/quarterly-outstanding")
    suspend fun getQuarterlyOutstanding(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): MonthlyOutstanding? {
        return Response<MonthlyOutstanding?>().ok(dashboardService.getMonthlyOutstanding(zone, role))
    }

    @Get("/outstanding-stats/add")
    suspend fun getOutstandingByAge() { return dashboardService.addMonthlyOutstandingTrend() }

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }

//    @Get("/sales-trend")
//    suspend fun getSalesTrend(
//        @QueryValue("zone") zone: String?,
//        @QueryValue("role") role: String?
//    ): List<SalesTrend> {
//        return Response<List<SalesTrend>>().ok(dashboardService.getSalesTrend(zone, role))
//    }
}
