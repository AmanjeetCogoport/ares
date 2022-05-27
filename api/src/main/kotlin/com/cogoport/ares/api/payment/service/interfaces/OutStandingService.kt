package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.OutstandingListRequest
import com.cogoport.ares.model.payment.OutstandingList

interface OutStandingService {
    suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList?
}
