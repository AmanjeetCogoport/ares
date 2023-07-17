package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.common.InvoiceBalanceAmountReq
import com.cogoport.ares.model.common.InvoiceBalanceResponse
import com.cogoport.ares.model.payment.event.DeleteInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusRequest
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.InvoicePaymentRequest
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import com.cogoport.ares.model.payment.response.InvoicePaymentResponse

interface AccountUtilizationService {
    suspend fun add(accUtilizationRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse>
    suspend fun add(accUtilizationRequest: AccUtilizationRequest): CreateInvoiceResponse
    suspend fun delete(request: DeleteInvoiceRequest): Boolean
    suspend fun update(updateInvoiceRequest: UpdateInvoiceRequest)
    suspend fun updateStatus(updateInvoiceStatusRequest: UpdateInvoiceStatusRequest)
    suspend fun getInvoicePaymentStatus(invoiceRequest: InvoicePaymentRequest): InvoicePaymentResponse?
    suspend fun getInvoicesNotPresentInAres(): List<Long>?
    suspend fun getInvoicesAmountMismatch(): List<Long>?
    suspend fun deleteInvoicesNotPresentInPlutus(id: Long)

    suspend fun getInvoiceBalanceAmount(requests: InvoiceBalanceAmountReq): List<InvoiceBalanceResponse>?
}
