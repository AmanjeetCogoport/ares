package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.model.payment.event.DeleteInvoiceRequest
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/invoice")
class InvoiceController {

    @Inject
    lateinit var accUtilService: AccountUtilizationService

    @Post("/add-bulk")
    suspend fun addBulkInvoice(@Valid @Body invoiceRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse> {
        return accUtilService.add(invoiceRequestList)
    }

    @Post
    suspend fun addInvoice(@Valid @Body invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {
        return accUtilService.add(invoiceRequest)
    }

    @Delete
    suspend fun deleteInvoice(@Valid @Body deleteRequest: DeleteInvoiceRequest): Boolean {
        return accUtilService.delete(deleteRequest)
    }

}
