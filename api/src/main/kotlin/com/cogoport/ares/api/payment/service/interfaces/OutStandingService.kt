package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.SupplierOutstandingResponse
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.BillOutstandingList
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import java.math.BigDecimal

interface OutStandingService {

    suspend fun getInvoiceList(request: InvoiceListRequest): ListInvoiceResponse?

    suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList?

    suspend fun getCustomerOutstanding(orgId: String): MutableList<CustomerOutstanding?>

    suspend fun getCurrOutstanding(invoiceIds: List<Long>): Long

    suspend fun getCustomersOutstandingInINR(orgIds: List<String>): MutableMap<String, BigDecimal?>

    suspend fun getSupplierOutstandingList(request: OutstandingListRequest): BillOutstandingList

    suspend fun updateSupplierOutstanding(orgId: String)

    suspend fun listSupplierOutstanding(request: SupplierOutstandingRequest): ResponseList<SupplierOutstandingResponse?>
}
