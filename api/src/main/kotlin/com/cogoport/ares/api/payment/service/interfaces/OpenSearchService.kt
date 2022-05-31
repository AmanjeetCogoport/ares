package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.OpenSearchRequest

interface OpenSearchService {

    suspend fun pushDashboardData(request: OpenSearchRequest)

    suspend fun pushOutstandingData(request: OpenSearchRequest)
}
