package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.OutstandingListRequest
import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList

interface OutStandingService {
    suspend fun getInvoiceList(zone: String?, orgId: String?, page: Int, page_limit: Int): MutableList<CustomerInvoiceResponse>?
    suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList?
}
