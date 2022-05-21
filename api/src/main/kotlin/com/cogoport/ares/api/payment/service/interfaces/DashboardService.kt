package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.SalesTrendResponse
import com.cogoport.ares.model.payment.CollectionTrend

import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OverallStats
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.ReceivableAgeingResponse

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
