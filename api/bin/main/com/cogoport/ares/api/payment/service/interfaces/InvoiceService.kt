package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse

interface InvoiceService {
    suspend fun addInvoice(invoiceRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse>
    suspend fun deleteInvoice(docId: Long, accType: String): Boolean
}
