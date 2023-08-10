package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.common.models.InvoiceTatStatsResponse
import com.cogoport.ares.api.common.models.OutstandingOpensearchResponse
import com.cogoport.ares.api.common.models.SalesFunnelResponse
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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

interface DashboardService {

    suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding?
    suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding?
    suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): LinkedHashMap<String, OverallAgeingStatsResponse>
    suspend fun deleteIndex(index: String)
    suspend fun createIndex(index: String)
    suspend fun getOrgCollection(request: OrganizationReceivablesRequest): List<OutstandingResponse>
    suspend fun getOrgPayables(request: OrgPayableRequest): OrgPayableResponse
    suspend fun getOverallStats(request: KamPaymentRequest): StatsForKamResponse
    suspend fun getOverallStatsForCustomers(request: CustomerStatsRequest): ResponseList<StatsForCustomerResponse?>

    suspend fun getStatsForTradeParties(request: TradePartyStatsRequest): ResponseList<OverallStatsForTradeParty?>

    suspend fun getInvoiceListForTradeParties(request: InvoiceListRequestForTradeParty): ResponseList<InvoiceListResponse?>

    suspend fun getSalesFunnel(req: SalesFunnelRequest): SalesFunnelResponse?

    suspend fun getInvoiceTatStats(req: InvoiceTatStatsRequest): InvoiceTatStatsResponse?

    suspend fun getDailySalesStatistics(req: DailyStatsRequest): HashMap<String, ArrayList<DailySalesStats>>

    suspend fun getOutstanding(entityCode: Int?): OutstandingOpensearchResponse?

    suspend fun getKamWiseOutstanding(entityCode: Int?, companyType: CompanyType?, serviceType: ServiceType?): List<KamWiseOutstanding>?

    suspend fun getLineGraphViewDailyStats(req: DailyStatsRequest): HashMap<String, ArrayList<DailySalesStats>>

    // Bf DashBoard Functions

    suspend fun getFinanceReceivableData(request: BfPendingAmountsReq): BfReceivableAndPayable

    suspend fun getFinanceIncomeExpense(request: BfIncomeExpenseReq): MutableList<BfIncomeExpenseResponse>

    suspend fun getFinanceTodayStats(request: BfTodayStatReq): BfTodayStatsResp

    suspend fun getFinanceShipmentProfit(request: BfProfitabilityReq): ShipmentProfitResp

    suspend fun getFinanceCustomerProfit(request: BfProfitabilityReq): ShipmentProfitResp

    suspend fun getFinanceServiceWiseRecPay(request: ServiceWiseRecPayReq): MutableList<ServiceWiseRecPayResp>

    suspend fun getFinanceServiceWiseOverdue(request: BfServiceWiseOverdueReq): ServiceWiseOverdueResp

    suspend fun getCustomersOverallStats(request: OverallStatsForCustomers): ResponseList<StatsForCustomerResponse?>
}
