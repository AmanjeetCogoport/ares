package com.cogoport.ares.payment.service.interfaces

import com.cogoport.ares.payment.model.OutstandingList

interface OutStandingService {
    suspend fun getOutstandingList(zone: String?, role: String?): OutstandingList?
}
