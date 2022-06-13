package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.model.payment.event.UpdateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusRequest

interface AccountUtilizationService {
    suspend fun add(accUtilizationRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse>
    suspend fun add(accUtilizationRequest: AccUtilizationRequest): CreateInvoiceResponse
    suspend fun delete(data: MutableList<Pair<Long, String>>): Boolean
    suspend fun findByDocumentNo(docNumber: Long): AccountUtilization
    suspend fun update(updateInvoiceRequest: UpdateInvoiceRequest)
    suspend fun updateStatus(updateInvoiceStatusRequest: UpdateInvoiceStatusRequest)
}
