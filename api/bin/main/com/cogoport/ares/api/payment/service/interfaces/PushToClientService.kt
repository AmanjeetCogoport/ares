package com.cogoport.ares.api.payment.service.interfaces

import java.util.*

interface PushToClientService {

    suspend fun pushDashboardData(zone: String?, date: String)
    suspend fun pushOutstandingData(zone: String?, orgId: String)

}
