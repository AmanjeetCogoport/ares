package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest

interface OutStandingService {

    suspend fun getInvoiceList(request: InvoiceListRequest): ListInvoiceResponse?

    suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList?

    suspend fun getCustomerOutstanding(orgId: String): MutableList<CustomerOutstanding?>

    suspend fun getCurrOutstanding(invoiceIds: List<Long>): Long
}
