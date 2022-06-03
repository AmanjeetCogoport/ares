package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.InvoiceListRequest
import com.cogoport.ares.model.payment.OutstandingListRequest
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.OutstandingList

interface OutStandingService {

    suspend fun getInvoiceList(request: InvoiceListRequest): ListInvoiceResponse?

    suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList?

    suspend fun getCustomerOutstanding(orgId: String): MutableList<CustomerOutstanding?>
}
