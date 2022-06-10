package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.service.interfaces.InvoiceService
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/invoice")
class InvoiceController {

    @Inject
    lateinit var invoiceService: InvoiceService

    @Post("/add-bulk")
    suspend fun addBulkInvoice(@Valid @Body invoiceRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse> {
        return invoiceService.addInvoice(invoiceRequestList)
    }

    @Post
    suspend fun addInvoice(@Valid @Body invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {
        return invoiceService.addAccountUtilization(invoiceRequest)
    }

    @Delete
    suspend fun deleteInvoice(@QueryValue("docNumber") docNumber: Long, @QueryValue("accType") accType: String): Boolean {
        return invoiceService.deleteInvoice(docNumber, accType)
    }
}
