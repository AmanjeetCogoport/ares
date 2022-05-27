package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.PushToDashboardRequest

interface PushToClientService {

    suspend fun pushDashboardData(request: PushToDashboardRequest)
    suspend fun pushOutstandingData(zone: String?, orgId: String)
}
