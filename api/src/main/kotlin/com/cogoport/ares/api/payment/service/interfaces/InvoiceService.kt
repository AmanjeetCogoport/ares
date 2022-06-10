package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.model.payment.event.CreateInvoiceRequest
import com.cogoport.ares.model.payment.event.UpdateInvoiceStatusRequest

interface InvoiceService {
    suspend fun addInvoice(accUtilizationRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse>
    suspend fun addAccountUtilization(accUtilizationRequest: AccUtilizationRequest): CreateInvoiceResponse
    suspend fun deleteInvoice(docNumber: Long, accType: String): Boolean
    suspend fun findByDocumentNo(docNumber: Long): AccountUtilization
    suspend fun updateInvoice(accUtilizationRequest: AccUtilizationRequest)
    suspend fun updateInvoiceStatus(updateInvoiceStatusRequest: UpdateInvoiceStatusRequest)
    suspend fun deleteCreateInvoice(createInvoiceRequest: CreateInvoiceRequest)
}
