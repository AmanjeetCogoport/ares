package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.event.UpdateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusRequest
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse

interface AccountUtilizationService {
    suspend fun add(accUtilizationRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse>
    suspend fun add(accUtilizationRequest: AccUtilizationRequest): CreateInvoiceResponse
    suspend fun delete(data: MutableList<Pair<Long, String>>): Boolean
    suspend fun update(updateInvoiceRequest: UpdateInvoiceRequest)
    suspend fun updateStatus(updateInvoiceStatusRequest: UpdateInvoiceStatusRequest)
}
