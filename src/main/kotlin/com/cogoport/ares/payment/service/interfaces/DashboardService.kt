package com.cogoport.ares.payment.service.interfaces

import com.cogoport.ares.payment.model.CollectionTrend
import com.cogoport.ares.payment.model.OutstandingByAge

interface DashboardService {

    suspend fun getOutstandingByAge(zone: String?, role: String?, quarter: String): OutstandingByAge?

    suspend fun addMonthlyOutstandingTrend()

    suspend fun deleteIndex(index: String)

    suspend fun createIndex(index: String)

    suspend fun getCollectionTrend(zone: String?, role: String?, quarter: String): CollectionTrend?
}
