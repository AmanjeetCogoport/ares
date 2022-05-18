package com.cogoport.ares.payment.service.interfaces

import com.cogoport.ares.payment.model.*

interface DashboardService {

    suspend fun getOverallOutstanding(zone: String?, role: String?): OverallOutstandingStats?

    suspend fun addMonthlyOutstandingTrend()

    suspend fun deleteIndex(index: String)

    suspend fun createIndex(index: String)

    suspend fun getCollectionTrend(zone: String?, role: String?, quarter: String): CollectionTrend?

    suspend fun getMonthlyOutstanding(zone: String?, role: String?): MonthlyOutstanding?

    suspend fun getQuarterlyOutstanding(zone: String?, role: String?): QuarterlyOutstanding?
//    suspend fun getSalesTrend(zone: String?, role: String?): List<SalesTrend>
}
