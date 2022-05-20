package com.cogoport.ares.payment.service.interfaces

import com.cogoport.ares.payment.model.AgeingBucket
import com.cogoport.ares.payment.model.CollectionTrend
import com.cogoport.ares.payment.model.DailySalesOutstanding
import com.cogoport.ares.payment.model.MonthlyOutstanding
import com.cogoport.ares.payment.model.OverallStats
import com.cogoport.ares.payment.model.QuarterlyOutstanding
import com.cogoport.ares.payment.model.ReceivableAgeingResponse
import com.cogoport.ares.payment.model.SalesTrendResponse

interface DashboardService {

    suspend fun getOverallStats(zone: String?, role: String?): OverallStats?

    suspend fun pushDataToOpenSearch()

    suspend fun deleteIndex(index: String)

    suspend fun createIndex(index: String)

    suspend fun getCollectionTrend(zone: String?, role: String?, quarter: String): CollectionTrend?

    suspend fun getMonthlyOutstanding(zone: String?, role: String?): MonthlyOutstanding?

    suspend fun getQuarterlyOutstanding(zone: String?, role: String?): QuarterlyOutstanding?
    suspend fun getSalesTrend(zone: String?, role: String?): SalesTrendResponse?

    suspend fun getDailySalesOutstanding(zone: String?, role: String?, quarter: String): DailySalesOutstanding?

    suspend fun getOutStandingByAge(): List<AgeingBucket>

    suspend fun getReceivableByAge(zone: String?, role: String?): ReceivableAgeingResponse
}
