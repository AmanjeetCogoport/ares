package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.entity.BfReceivableAndPayable
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

interface DashboardService {

    suspend fun getOverallStats(request: OverallStatsRequest): OverallStatsResponseData?
    suspend fun getCollectionTrend(request: CollectionRequest): CollectionResponse?
    suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding?
    suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding?
    suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding?
    suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse>
    suspend fun getReceivableByAge(request: ReceivableRequest): HashMap<String, ArrayList<AgeingBucketZone>>
    suspend fun deleteIndex(index: String)
    suspend fun createIndex(index: String)
    suspend fun getOrgCollection(request: OrganizationReceivablesRequest): List<OutstandingResponse>
    suspend fun getOrgPayables(request: OrgPayableRequest): OrgPayableResponse
    suspend fun getOverallStats(request: KamPaymentRequest): StatsForKamResponse
    suspend fun getOverallStatsForCustomers(request: CustomerStatsRequest): ResponseList<StatsForCustomerResponse?>

    suspend fun getStatsForTradeParties(request: TradePartyStatsRequest): ResponseList<OverallStatsForTradeParty?>

    suspend fun getInvoiceListForTradeParties(request: InvoiceListRequestForTradeParty): ResponseList<InvoiceListResponse?>

    // Bf DashBoard Functions

    suspend fun getBfReceivableData(request: BfPendingAmountsReq): BfReceivableAndPayable

    suspend fun getBfIncomeExpense(request: BfIncomeExpenseReq): MutableList<BfIncomeExpenseResponse>

    suspend fun getBfTodayStats(request: BfTodayStatReq): BfTodayStatsResp

    suspend fun getBfShipmentProfit(request: BfProfitabilityReq): ShipmentProfitResp

    suspend fun getBfCustomerProfit(request: BfProfitabilityReq): ShipmentProfitResp

    suspend fun getBfServiceWiseRecPay(entityCode: Int?): MutableList<ServiceWiseRecPayResp>

    suspend fun getServiceWiseOverdue(request: BfServiceWiseOverdueReq): ServiceWiseOverdueResp
}
