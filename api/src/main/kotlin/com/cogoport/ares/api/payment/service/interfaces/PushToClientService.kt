package com.cogoport.ares.api.payment.service.interfaces

interface PushToClientService {

    suspend fun pushDashboardData(zone: String?, date: String, quarter: Int?)
    suspend fun pushOutstandingData(zone: String?, orgId: String)

}
