package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.models.InvoiceTatStatsResponse
import com.cogoport.ares.api.common.models.OutstandingOpensearchResponse
import com.cogoport.ares.api.common.models.SalesFunnelResponse
import com.cogoport.ares.api.common.service.interfaces.ExchangeRateHelper
import com.cogoport.ares.api.payment.entity.BfReceivableAndPayable
import com.cogoport.ares.api.payment.entity.DailySalesStats
import com.cogoport.ares.api.payment.entity.KamWiseOutstanding
import com.cogoport.ares.api.payment.model.requests.BfIncomeExpenseReq
import com.cogoport.ares.api.payment.model.requests.BfPendingAmountsReq
import com.cogoport.ares.api.payment.model.requests.BfProfitabilityReq
import com.cogoport.ares.api.payment.model.requests.BfServiceWiseOverdueReq
import com.cogoport.ares.api.payment.model.requests.BfTodayStatReq
import com.cogoport.ares.api.payment.model.requests.ServiceWiseRecPayReq
import com.cogoport.ares.api.payment.model.response.BfIncomeExpenseResponse
import com.cogoport.ares.api.payment.model.response.BfTodayStatsResp
import com.cogoport.ares.api.payment.model.response.ServiceWiseOverdueResp
import com.cogoport.ares.api.payment.model.response.ServiceWiseRecPayResp
import com.cogoport.ares.api.payment.model.response.ShipmentProfitResp
import com.cogoport.ares.api.payment.service.interfaces.DashboardService
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.OverallStatsForCustomers
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.CompanyType
import com.cogoport.ares.model.payment.CustomerStatsRequest
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.KamPaymentRequest
import com.cogoport.ares.model.payment.OrgPayableRequest
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.request.DailyStatsRequest
import com.cogoport.ares.model.payment.request.ExchangeRateForPeriodRequest
import com.cogoport.ares.model.payment.request.InvoiceListRequestForTradeParty
import com.cogoport.ares.model.payment.request.InvoiceTatStatsRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.SalesFunnelRequest
import com.cogoport.ares.model.payment.request.TradePartyStatsRequest
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OrgPayableResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
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

    @Inject
    lateinit var util: Util

    @Auth
    @Get("/daily-sales-outstanding{?request*}")
    suspend fun getDailySalesOutstanding(
        @Valid request: DsoRequest,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): DailySalesOutstanding? {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return Response<DailySalesOutstanding?>().ok(dashboardService.getDailySalesOutstanding(request))
    }

    @Auth
    @Get("/quarterly-outstanding{?request*}")
    suspend fun getQuarterlyOutstanding(
        @Valid request: QuarterlyOutstandingRequest,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): QuarterlyOutstanding? {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return Response<QuarterlyOutstanding?>().ok(dashboardService.getQuarterlyOutstanding(request))
    }

    @Auth
    @Get("/outstanding-by-age{?request*}")
    suspend fun getOutStandingByAge(
        @Valid request: OutstandingAgeingRequest,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): LinkedHashMap<String, OverallAgeingStatsResponse> {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return Response<LinkedHashMap<String, OverallAgeingStatsResponse>>().ok(dashboardService.getOutStandingByAge(request))
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

    @Delete("/index")
    suspend fun deleteIndex(@QueryValue("name") name: String) { return dashboardService.deleteIndex(name) }

    @Get("/index")
    suspend fun createIndex(@QueryValue("name") name: String) { return dashboardService.createIndex(name) }

    @Auth
    @Get("/sales-funnel{?request*}")
    suspend fun getSalesFunnel(
        @Valid request: SalesFunnelRequest,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): SalesFunnelResponse? {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return dashboardService.getSalesFunnel(request)
    }

    @Auth
    @Get("/invoice-tat-stats{?request*}")
    suspend fun getInvoiceTatStats(
        @Valid request: InvoiceTatStatsRequest,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): InvoiceTatStatsResponse? {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return dashboardService.getInvoiceTatStats(request)
    }

    @Auth
    @Get("/daily-sales-statistics{?req*}")
    suspend fun getDailySalesStatistics(
        @Valid req: DailyStatsRequest,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): HashMap<String, ArrayList<DailySalesStats>> {
        req.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: req.entityCode
        return dashboardService.getDailySalesStatistics(req)
    }

    @Auth
    @Get("/outstanding")
    suspend fun getOutstanding(
        @QueryValue("entityCode") entityCode: Int? = 301,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): OutstandingOpensearchResponse? {
        val updatedEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: entityCode
        return dashboardService.getOutstanding(updatedEntityCode)
    }
    @Auth
    @Get("/kam-wise-outstanding")
    suspend fun getKamWiseOutstanding(
        @QueryValue("entityCode") entityCode: Int? = 301,
        @QueryValue("companyType") companyType: CompanyType?,
        @QueryValue("serviceType") serviceType: ServiceType?,
        user: AuthResponse?
    ): List<KamWiseOutstanding>? {
        val updatedEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: entityCode
        return dashboardService.getKamWiseOutstanding(updatedEntityCode, companyType, serviceType)
    }

    @Auth
    @Get("/line-graph-view{?req*}")
    suspend fun getLineGraphViewDailyStats(
        @Valid req: DailyStatsRequest,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): HashMap<String, ArrayList<DailySalesStats>> {
        req.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: req.entityCode
        return dashboardService.getLineGraphViewDailyStats(req)
    }

    // ** Business Finance DashBoard Apis */

    @Auth
    @Get("/finance-receivable-payable{?request*}")
    suspend fun getFinanceReceivableData(
        @Valid request: BfPendingAmountsReq,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): BfReceivableAndPayable {
        val authEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt()
        request.entityCode = if (authEntityCode == null) request.entityCode else mutableListOf(authEntityCode)
        return dashboardService.getFinanceReceivableData(request)
    }
    @Auth
    @Get("/finance-income-expense{?request*}")
    suspend fun getFinanceIncomeExpense(@Valid request: BfIncomeExpenseReq, user: AuthResponse?, httpRequest: HttpRequest<*>): MutableList<BfIncomeExpenseResponse> {
        val authEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt()
        request.entityCode = if (authEntityCode == null) request.entityCode else mutableListOf(authEntityCode)
        return dashboardService.getFinanceIncomeExpense(request)
    }

    @Auth
    @Get("/finance-today-stats{?request*}")
    suspend fun getFinanceTodayStats(
        @Valid request: BfTodayStatReq,
        user: AuthResponse?,
        httpRequest: HttpRequest<*>
    ): BfTodayStatsResp {
        val authEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt()
        request.entityCode = if (authEntityCode == null) request.entityCode else mutableListOf(authEntityCode)
        return dashboardService.getFinanceTodayStats(request)
    }

    @Auth
    @Get("/finance-profitability-shipment{?request*}")
    suspend fun getFinanceShipmentProfit(@Valid request: BfProfitabilityReq, user: AuthResponse?, httpRequest: HttpRequest<*>): ShipmentProfitResp {
        val authEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt()
        request.entityCode = if (authEntityCode == null) request.entityCode else mutableListOf(authEntityCode)
        return dashboardService.getFinanceShipmentProfit(request)
    }

    @Auth
    @Get("/finance-profitability-customer{?request*}")
    suspend fun getFinanceCustomerProfit(@Valid request: BfProfitabilityReq, user: AuthResponse?, httpRequest: HttpRequest<*>): ShipmentProfitResp {
        val authEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt()
        request.entityCode = if (authEntityCode == null) request.entityCode else mutableListOf(authEntityCode)
        return dashboardService.getFinanceCustomerProfit(request)
    }

    @Auth
    @Get("/finance-service-wise-rec-pay{?request*}")
    suspend fun getFinanceServiceWiseRecPay(@Valid request: ServiceWiseRecPayReq, user: AuthResponse?, httpRequest: HttpRequest<*>): MutableList<ServiceWiseRecPayResp> {
        val authEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt()
        request.entityCode = if (authEntityCode == null) request.entityCode else mutableListOf(authEntityCode)
        return dashboardService.getFinanceServiceWiseRecPay(request)
    }

    @Auth
    @Get("/finance-service-wise-overdue{?request*}")
    suspend fun getFinanceServiceWiseOverdue(request: BfServiceWiseOverdueReq, user: AuthResponse?, httpRequest: HttpRequest<*>): ServiceWiseOverdueResp {
        val authEntityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt()
        request.entityCode = if (authEntityCode == null) request.entityCode else mutableListOf(authEntityCode)
        return dashboardService.getFinanceServiceWiseOverdue(request)
    }

    @Post("/customers/overall-stats")
    suspend fun getOverallCustomersStats(@Valid @Body request: OverallStatsForCustomers): ResponseList<StatsForCustomerResponse?> {
        return dashboardService.getCustomersOverallStats(request)
    }
}
