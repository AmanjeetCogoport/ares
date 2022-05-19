package com.cogoport.ares.payment.service.interfaces

import com.cogoport.ares.payment.model.AccUtilizationRequest
import com.cogoport.ares.payment.model.CreateInvoiceResponse
import org.apache.kafka.common.protocol.types.Field

interface InvoiceService {
    suspend fun addInvoice(invoiceRequest: AccUtilizationRequest):CreateInvoiceResponse
    suspend fun deleteInvoice(docId:Long,accType:String):Boolean
}