package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.CollectionRequest
import com.cogoport.ares.api.payment.model.DsoRequest
import com.cogoport.ares.api.payment.model.MonthlyOutstandingRequest
import com.cogoport.ares.api.payment.model.OutstandingAgeingRequest
import com.cogoport.ares.api.payment.model.OverallStatsRequest
import com.cogoport.ares.api.payment.model.QuarterlyOutstandingRequest
import com.cogoport.ares.api.payment.model.ReceivableRequest
import com.cogoport.ares.api.payment.model.SalesTrendRequest
import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.OverallStatsResponse
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.SalesTrend

interface DashboardService {

    suspend fun getOverallStats(request: OverallStatsRequest): OverallStatsResponse?
    suspend fun getCollectionTrend(request: CollectionRequest): CollectionResponse?
    suspend fun getMonthlyOutstanding(request: MonthlyOutstandingRequest): MonthlyOutstanding?
    suspend fun getQuarterlyOutstanding(request: QuarterlyOutstandingRequest): QuarterlyOutstanding?
    suspend fun getDailySalesOutstanding(request: DsoRequest): DailySalesOutstanding?
    suspend fun getOutStandingByAge(request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse>
    suspend fun getReceivableByAge(request: ReceivableRequest): ReceivableAgeingResponse
    suspend fun getSalesTrend(request: SalesTrendRequest): MutableList<SalesTrend>
    suspend fun deleteIndex(index: String)
    suspend fun createIndex(index: String)
}
