package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.models.InvoiceTatStatsResponse
import com.cogoport.ares.api.common.models.OutstandingOpensearchResponse
import com.cogoport.ares.api.common.models.SalesFunnelResponse
import com.cogoport.ares.api.common.service.interfaces.ExchangeRateHelper
import com.cogoport.ares.api.payment.entity.DailySalesStats
import com.cogoport.ares.api.payment.entity.KamWiseOutstanding
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AgeingBucketZone
import com.cogoport.ares.model.payment.CustomerStatsRequest
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.KamPaymentRequest
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OrgPayableRequest
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.request.CollectionRequest
import com.cogoport.ares.model.payment.request.DailyStatsRequest
import com.cogoport.ares.model.payment.request.ExchangeRateForPeriodRequest
import com.cogoport.ares.model.payment.request.InvoiceListRequestForTradeParty
import com.cogoport.ares.model.payment.request.InvoiceTatStatsRequest
import com.cogoport.ares.model.payment.request.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.OverallStatsRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.ReceivableRequest
import com.cogoport.ares.model.payment.request.SalesFunnelRequest
import com.cogoport.ares.model.payment.request.TradePartyStatsRequest
import com.cogoport.ares.model.payment.response.CollectionResponse
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OrgPayableResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.OverallStatsResponseData
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.math.BigDecimal
import java.util.UUID
import javax.validation.Valid
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Validated
@Controller("/dashboard")
class DashboardController {
    @Inject
    lateinit var dashboardService: DashboardService
    @Inject
    lateinit var pushToClientService: OpenSearchService
    @Inject
    lateinit var exchangeRateHelper: ExchangeRateHelper
    @Get("/overall-stats{?request*}")
    suspend fun getOverallStats(@Valid request: OverallStatsRequest): OverallStatsResponseData? {
        return Response<OverallStatsResponseData?>().ok(dashboardService.getOverallStats(request))
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

    @Get("/outstanding-by-age{?request*}")
    suspend fun getOutStandingByAge(@Valid request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse>? {
        return Response<List<OverallAgeingStatsResponse>?>().ok(dashboardService.getOutStandingByAge(request))
    }

    @Get("/receivables-by-age{?request*}")
    suspend fun getReceivablesByAge(@Valid request: ReceivableRequest): HashMap<String, ArrayList<AgeingBucketZone>> {
        return Response<HashMap<String, ArrayList<AgeingBucketZone>>>().ok(dashboardService.getReceivableByAge(request))
    }

    @Get("/org-collection{?request*}")
    suspend fun getOrgCollection(@Valid request: OrganizationReceivablesRequest): List<OutstandingResponse> {
        return Response<List<OutstandingResponse>>().ok(dashboardService.getOrgCollection(request))
    }

    @Get("/org-payables-stats{?request*}")
    suspend fun getOrgPayables(@Valid request: OrgPayableRequest): OrgPayableResponse {
        return Response<OrgPayableResponse>().ok(dashboardService.getOrgPayables(request))
    }

    @Post("/kam/overall-stats")
    suspend fun getOverallStatsForKam(@Valid @Body request: KamPaymentRequest): StatsForKamResponse {
        return Response<StatsForKamResponse>().ok(dashboardService.getOverallStats(request))
    }

    @Post("/customer/overall-stats")
    suspend fun getOverallStatsForCustomers(
        @Valid @Body request: CustomerStatsRequest
    ): ResponseList<StatsForCustomerResponse?> {
        return dashboardService.getOverallStatsForCustomers(request)
    }

    @Post("/trade-party/stats")
    suspend fun getOverallStatsForTradeParties(
        @Valid @Body request: TradePartyStatsRequest
    ): ResponseList<OverallStatsForTradeParty?> {
        return dashboardService.getStatsForTradeParties(request)
    }

    @Post("/trade-party/invoice/list")
    suspend fun getInvoiceListForTradeParties(
        @Valid @Body request: InvoiceListRequestForTradeParty
    ): ResponseList<InvoiceListResponse?> {
        return dashboardService.getInvoiceListForTradeParties(request)
    }

    @Get("/exchange-rate/for/period{?request*}")
    suspend fun getExchangeRateForPeriod(@Valid request: ExchangeRateForPeriodRequest): HashMap<String, BigDecimal> {
        return exchangeRateHelper.getExchangeRateForPeriod(request.currencyList, request.dashboardCurrency)
    }

    /** To be Deleted */

    @Get("/open-search/add{?request*}")
    suspend fun addToOpenSearch(@Valid request: OpenSearchRequest) { return pushToClientService.pushDashboardData(request) }

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }

    @Get("/sales-funnel{?request*}")
    suspend fun getSalesFunnel(
        @Valid request: SalesFunnelRequest
    ): SalesFunnelResponse? {
        return dashboardService.getSalesFunnel(request)
    }

    @Get("/invoice-tat-stats{?request*}")
    suspend fun getInvoiceTatStats(
        @Valid request: InvoiceTatStatsRequest
    ): InvoiceTatStatsResponse? {
        return dashboardService.getInvoiceTatStats(request)
    }

    @Get("/daily-sales-statistics{?req*}")
    suspend fun getDailySalesStatistics(@Valid req: DailyStatsRequest): HashMap<String, ArrayList<DailySalesStats>> {
        return dashboardService.getDailySalesStatistics(req)
    }

    @Get("/outstanding")
    suspend fun getOutstanding(
        @QueryValue("date") date: String? = null,
        @QueryValue("cogoEntityId") cogoEntityId: UUID?,
    ): OutstandingOpensearchResponse? {
        return dashboardService.getOutstanding(date, cogoEntityId)
    }

    @Get("/kam-wise-outstanding")
    suspend fun getKamWiseOutstanding(): List<KamWiseOutstanding>? {
        return dashboardService.getKamWiseOutstanding()
    }

    @Get("/line-graph-view{?req*}")
    suspend fun getLineGraphViewDailyStats(@Valid req: DailyStatsRequest): HashMap<String, ArrayList<DailySalesStats>> {
        return dashboardService.getLineGraphViewDailyStats(req)
    }
}
