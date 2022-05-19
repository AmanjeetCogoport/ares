package com.cogoport.ares.payment.service.implementation

import com.cogoport.ares.payment.model.AccUtilizationRequest
import com.cogoport.ares.payment.model.CreateInvoiceResponse
import com.cogoport.ares.payment.service.interfaces.InvoiceService

class InvoiceUtilization:InvoiceService {

    override suspend fun addInvoice(invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {

      return CreateInvoiceResponse(1,"OK")
    }
}