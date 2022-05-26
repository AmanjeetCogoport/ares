package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.OverallStatsResponse
import com.cogoport.ares.model.payment.SalesTrendResponse
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
interface DashboardService {

    suspend fun getOverallStats(zone: String?, role: String?): OverallStatsResponse?
    suspend fun getCollectionTrend(zone: String?, role: String?, quarter: Int, year: Int): CollectionResponse?
    suspend fun getMonthlyOutstanding(zone: String?, role: String?): MonthlyOutstanding?
    suspend fun getQuarterlyOutstanding(zone: String?, role: String?): QuarterlyOutstanding?
    suspend fun getSalesTrend(zone: String?, role: String?): SalesTrendResponse?
    suspend fun getDailySalesOutstanding(zone: String?, role: String?, quarter: List<Int>, year: Int): DailySalesOutstanding?
    suspend fun getOutStandingByAge(zone: String?, role: String?): MutableList<OverallAgeingStatsResponse>
    suspend fun getReceivableByAge(zone: String?, role: String?): ReceivableAgeingResponse
    suspend fun deleteIndex(index: String)
    suspend fun createIndex(index: String)
}
