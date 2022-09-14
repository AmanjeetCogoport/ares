package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OrgPayableRequest
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.request.CollectionRequest
import com.cogoport.ares.model.payment.request.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.OverallStatsRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.ReceivableRequest
import com.cogoport.ares.model.payment.response.*

interface DashboardService {

    suspend fun getOverallStats(request: OverallStatsRequest): OverallStatsResponse?
    suspend fun getCollectionTrend(request: CollectionRequest): CollectionResponse?
    suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding?
    suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding?
    suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding?
    suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse>
    suspend fun getReceivableByAge(request: ReceivableRequest): ReceivableAgeingResponse
    suspend fun deleteIndex(index: String)
    suspend fun createIndex(index: String)
    suspend fun getOrgCollection(request: OrganizationReceivablesRequest): List<OutstandingResponse>
    suspend fun getOrgPayables(request: OrgPayableRequest): OrgPayableResponse
    suspend fun getOverallStatsForKam(docValue: List<String>): OverallStatsForKamResponse
    suspend fun getOverallStatsForCustomer(docValue: List<String>, custId: String): OverallStatsForCustomerResponse
}
