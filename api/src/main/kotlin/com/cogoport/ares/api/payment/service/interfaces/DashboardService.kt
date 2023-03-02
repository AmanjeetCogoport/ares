package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.common.models.DailyStatsResponse
import com.cogoport.ares.api.common.models.InvoiceTimeLineResponse
import com.cogoport.ares.api.common.models.OutstandingOpensearchResponse
import com.cogoport.ares.api.common.models.SalesFunnelResponse
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AgeingBucketZone
import com.cogoport.ares.model.payment.CustomerStatsRequest
import com.cogoport.ares.model.payment.DailySalesAndQuarterlyOutstanding
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
import java.util.Date

interface DashboardService {

    suspend fun getOverallStats(request: OverallStatsRequest): OverallStatsResponseData?
    suspend fun getCollectionTrend(request: CollectionRequest): CollectionResponse?
    suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding?
    suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding?
    suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesAndQuarterlyOutstanding?
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

    suspend fun getSalesFunnel (month: String?): SalesFunnelResponse?

    suspend fun getInvoiceTimeline (startDate: String? , endDate: String?): InvoiceTimeLineResponse?

    suspend fun getDailySalesStatistics (month: String?, year:Int?, asOnDate: String?, documentType: String?): DailyStatsResponse

    suspend fun getOutstanding (date: String?): OutstandingOpensearchResponse?
}
