package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.BillOutstandingList
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import java.math.BigDecimal

interface OutStandingService {

    suspend fun getInvoiceList(request: InvoiceListRequest): ListInvoiceResponse?

    suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList?

    suspend fun getCustomerOutstanding(orgId: String): MutableList<CustomerOutstanding?>

    suspend fun getCurrOutstanding(invoiceIds: List<Long>): Long

    suspend fun getCustomersOutstandingInINR(orgIds: List<String>): MutableMap<String, BigDecimal?>

    suspend fun getBillsOutstandingList(request: OutstandingListRequest): BillOutstandingList

    suspend fun updateSupplierOutStanding(orgId: String)
}
