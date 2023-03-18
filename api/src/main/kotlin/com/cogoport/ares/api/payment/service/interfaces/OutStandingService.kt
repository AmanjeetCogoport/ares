package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentRequest
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.ListInvoiceResponse
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.SupplierOutstandingList
import com.cogoport.ares.model.payment.request.InvoiceListRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.request.SupplierOutstandingRequest
import com.cogoport.ares.model.payment.response.SupplierOutstandingDocument
import java.math.BigDecimal

interface OutStandingService {

    suspend fun getInvoiceList(request: InvoiceListRequest): ListInvoiceResponse?

    suspend fun getOutstandingList(request: OutstandingListRequest): OutstandingList?

    suspend fun getCustomerOutstanding(orgId: String): MutableList<CustomerOutstanding?>

    suspend fun getCurrOutstanding(invoiceIds: List<Long>): Long

    suspend fun getCustomersOutstandingInINR(orgIds: List<String>): MutableMap<String, BigDecimal?>

    suspend fun getSupplierOutstandingList(request: OutstandingListRequest): SupplierOutstandingList

    suspend fun createSupplierDetails(request: SupplierOutstandingDocument)
    suspend fun updateSupplierDetails(id: String, flag: Boolean, document: SupplierOutstandingDocument?)

    suspend fun listSupplierDetails(request: SupplierOutstandingRequest): ResponseList<SupplierOutstandingDocument?>

    suspend fun getCustomerOutstandingPaymentDetails(request: CustomerOutstandingPaymentRequest): ResponseList<CustomerOutstandingPaymentResponse?>
}
