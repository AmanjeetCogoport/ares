package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.model.PushToDashboardRequest
import com.cogoport.ares.api.payment.model.CollectionRequest
import com.cogoport.ares.api.payment.model.DsoRequest
import com.cogoport.ares.api.payment.model.OverallStatsRequest
import com.cogoport.ares.api.payment.model.MonthlyOutstandingRequest
import com.cogoport.ares.api.payment.model.QuarterlyOutstandingRequest
import com.cogoport.ares.api.payment.model.OutstandingAgeingRequest
import com.cogoport.ares.api.payment.model.ReceivableRequest
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.PushToClientService
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OverallStatsResponse
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
        return Response<OverallStatsResponse?>().ok(dashboardService.getOverallStats(request))
    }

    @Get("/daily-sales-outstanding{?request*}")
    suspend fun getDailySalesOutstandingWidget(request: DsoRequest): DailySalesOutstanding? {
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
    suspend fun getOutStandingByAge(@Valid request: OutstandingAgeingRequest): MutableList<OverallAgeingStatsResponse>? {
        return Response<MutableList<OverallAgeingStatsResponse>?>().ok(dashboardService.getOutStandingByAge(request))
    }
    @Get("/receivables-by-age{?request*}")
    suspend fun getReceivablesByAge(@Valid request: ReceivableRequest): ReceivableAgeingResponse {
        return Response<ReceivableAgeingResponse>().ok(dashboardService.getReceivableByAge(request))
    }

    /** To be Deleted */

    @Get("/open-search/add")
    suspend fun addToOpenSearch(request: PushToDashboardRequest) { return pushToClientService.pushDashboardData(request) }

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }
}
