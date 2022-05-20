package com.cogoport.ares.payment.service.interfaces

import com.cogoport.ares.payment.model.AccUtilizationRequest
import com.cogoport.ares.payment.model.CreateInvoiceResponse

interface InvoiceService {
    suspend fun addInvoice(invoiceRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse>
    suspend fun deleteInvoice(docId: Long, accType: String): Boolean
}
