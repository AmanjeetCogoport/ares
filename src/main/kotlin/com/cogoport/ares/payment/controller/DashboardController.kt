package com.cogoport.ares.payment.controller

import com.cogoport.ares.common.models.Response
import com.cogoport.ares.payment.model.CollectionTrend
import com.cogoport.ares.payment.model.MonthlyOutstanding
import com.cogoport.ares.payment.model.OutstandingByAge
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

    @Get("/outstanding-by-age")
    suspend fun getOutstandingByAge(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): OutstandingByAge? {
        return Response<OutstandingByAge?>().ok(dashboardService.getOutstandingByAge(zone, role))
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

    @Get("/outstanding-by-age/add")
    suspend fun getOutstandingByAge() { return dashboardService.addMonthlyOutstandingTrend() }

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }
}
