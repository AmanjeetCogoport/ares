package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.service.interfaces.ExchangeRateHelper
import com.cogoport.ares.api.payment.entity.BfReceivableAndPayable
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.model.requests.BfIncomeExpenseReq
import com.cogoport.ares.api.payment.model.requests.BfPendingAmountsReq
import com.cogoport.ares.api.payment.model.requests.BfProfitabilityReq
import com.cogoport.ares.api.payment.model.requests.BfServiceWiseOverdueReq
import com.cogoport.ares.api.payment.model.requests.BfTodayStatReq
import com.cogoport.ares.api.payment.model.response.BfIncomeExpenseResponse
import com.cogoport.ares.api.payment.model.response.BfTodayStatsResp
import com.cogoport.ares.api.payment.model.response.ServiceWiseOverdueResp
import com.cogoport.ares.api.payment.model.response.ServiceWiseRecPayResp
import com.cogoport.ares.api.payment.model.response.ShipmentProfitResp
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.utils.Util
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
import com.cogoport.ares.model.payment.request.ExchangeRateForPeriodRequest
import com.cogoport.ares.model.payment.request.InvoiceListRequestForTradeParty
import com.cogoport.ares.model.payment.request.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.OverallStatsRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.ReceivableRequest
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
import com.cogoport.brahma.authentication.Auth
import com.cogoport.brahma.authentication.AuthResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.math.BigDecimal
import javax.validation.Valid

@Validated
@Controller("/dashboard")
class DashboardController {
    @Inject
    lateinit var dashboardService: DashboardService
    @Inject
    lateinit var pushToClientService: OpenSearchService
    @Inject
    lateinit var exchangeRateHelper: ExchangeRateHelper

    @Inject
    lateinit var util: Util
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

    @Get("outstanding-by-age{?request*}")
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

    // ** Business Finance DashBoard Apis */

    @Auth
    @Get("/bf-receivable{?request*}")
    suspend fun getBfReceivableData(
        @Valid request: BfPendingAmountsReq,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): BfReceivableAndPayable {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return dashboardService.getBfReceivableData(request)
    }
    @Auth
    @Get("/bf-income-expense{?request*}")
    suspend fun getBfIncomeExpense(@Valid request: BfIncomeExpenseReq, user: AuthResponse?, httpRequest: HttpRequest<*>): MutableList<BfIncomeExpenseResponse> {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return dashboardService.getBfIncomeExpense(request)
    }

    @Auth
    @Get("/bf-today-stats{?request*}")
    suspend fun getBfTodayStats(
        @Valid request: BfTodayStatReq,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): BfTodayStatsResp {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return dashboardService.getBfTodayStats(request)
    }

    @Auth
    @Get("/bf-profitability-shipment{?request*}")
    suspend fun getBfShipmentProfit(@Valid request: BfProfitabilityReq, user: AuthResponse?, httpRequest: HttpRequest<*>): ShipmentProfitResp {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return dashboardService.getBfShipmentProfit(request)
    }

    @Auth
    @Get("/bf-profitability-customer{?request*}")
    suspend fun getBfCustomerProfit(@Valid request: BfProfitabilityReq, user: AuthResponse?, httpRequest: HttpRequest<*>): ShipmentProfitResp {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return dashboardService.getBfCustomerProfit(request)
    }

    @Auth
    @Get("bf-service-wise-rec-pay")
    suspend fun getBfServiceWiseRecPay(@QueryValue("entityCode") entityCode: Int? = null, user: AuthResponse?, httpRequest: HttpRequest<*>): MutableList<ServiceWiseRecPayResp> {
        val computedEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: entityCode
        return dashboardService.getBfServiceWiseRecPay(computedEntityCode)
    }

    @Auth
    @Get("bf-service-wise-overdue{?request*}")
    suspend fun getBfServiceWiseOverdue(request: BfServiceWiseOverdueReq, user: AuthResponse?, httpRequest: HttpRequest<*>): ServiceWiseOverdueResp {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return dashboardService.getServiceWiseOverdue(request)
    }

    /** To be Deleted */

    @Get("/open-search/add{?request*}")
    suspend fun addToOpenSearch(@Valid request: OpenSearchRequest) { return pushToClientService.pushDashboardData(request) }

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }
}
