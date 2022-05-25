package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.entity.OverallAgeingStats
import com.cogoport.ares.model.payment.*

interface DashboardService {

    suspend fun getOverallStats(zone: String?, role: String?): OverallStatsResponse?

    suspend fun pushDataToOpenSearch()

    suspend fun deleteIndex(index: String)

    suspend fun createIndex(index: String)

    suspend fun getCollectionTrend(zone: String?, role: String?, quarter: String): CollectionTrendResponse?

    suspend fun getMonthlyOutstanding(zone: String?, role: String?): MonthlyOutstanding?

    suspend fun getQuarterlyOutstanding(zone: String?, role: String?): QuarterlyOutstanding?
    suspend fun getSalesTrend(zone: String?, role: String?): SalesTrendResponse?

    suspend fun getDailySalesOutstanding(zone: String?, role: String?, quarter: String): DailySalesOutstanding?

    suspend fun getOutStandingByAge(zone: String?): MutableList<OverallAgeingStatsResponse>

    suspend fun getReceivableByAge(zone: String?, role: String?): ReceivableAgeingResponse
}
