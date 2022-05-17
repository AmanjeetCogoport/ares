package com.cogoport.ares.payment.service.interfaces

import com.cogoport.ares.payment.model.CollectionTrend
import com.cogoport.ares.payment.model.MonthlyOutstanding
import com.cogoport.ares.payment.model.OutstandingByAge
import com.cogoport.ares.payment.model.QuarterlyOutstanding

interface DashboardService {

    suspend fun getOutstandingByAge(zone: String?, role: String?): OutstandingByAge?

    suspend fun addMonthlyOutstandingTrend()

    suspend fun deleteIndex(index: String)

    suspend fun createIndex(index: String)

    suspend fun getCollectionTrend(zone: String?, role: String?, quarter: String): CollectionTrend?

    suspend fun getMonthlyOutstanding(zone: String?, role: String?): MonthlyOutstanding?

    suspend fun getQuarterlyOutstanding(zone: String?, role: String?): QuarterlyOutstanding?

}
