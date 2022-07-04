package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.model.payment.event.UpdateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusRequest
import java.util.UUID

interface AccountUtilizationService {
    suspend fun add(accUtilizationRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse>
    suspend fun add(accUtilizationRequest: AccUtilizationRequest): CreateInvoiceResponse
    suspend fun delete(data: MutableList<Pair<Long, String>>, performedBy: UUID?, performedByUserType: String?): Boolean
    suspend fun update(updateInvoiceRequest: UpdateInvoiceRequest)
    suspend fun updateStatus(updateInvoiceStatusRequest: UpdateInvoiceStatusRequest)
}
