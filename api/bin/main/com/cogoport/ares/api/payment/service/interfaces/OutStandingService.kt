package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.CustomerInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList

interface OutStandingService {
    suspend fun getOutstandingList(zone: String?, role: String?, quarter: Int?, page: Int, page_limit: Int): OutstandingList?
    suspend fun getInvoiceList(zone: String?, orgId: String?, page: Int, page_limit: Int): MutableList<CustomerInvoiceResponse>
}
