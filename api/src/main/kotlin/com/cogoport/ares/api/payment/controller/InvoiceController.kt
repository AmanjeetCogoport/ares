package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.service.implementation.Scheduler
import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.model.common.InvoiceBalanceResponse
import com.cogoport.ares.model.payment.event.DeleteInvoiceRequest
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.InvoicePaymentRequest
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import com.cogoport.ares.model.payment.response.InvoicePaymentResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/invoice")
class InvoiceController {

    @Inject
    lateinit var accUtilService: AccountUtilizationService

    @Inject
    lateinit var scheduler: Scheduler

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

    @Get("/payment-status{?invoicePaymentRequest*}")
    suspend fun getInvoicePaymentStatus(@Valid invoicePaymentRequest: InvoicePaymentRequest): InvoicePaymentResponse? {
        return accUtilService.getInvoicePaymentStatus(invoicePaymentRequest)
    }

    @Get("/missing-invoices")
    suspend fun getInvoicesNotPresentInAres(): List<Long>? {
        return accUtilService.getInvoicesNotPresentInAres()
    }

    @Get("/amount-mismatch")
    suspend fun getInvoicesAmountMismatch(): List<Long>? {
        return accUtilService.getInvoicesAmountMismatch()
    }

    @Put("/scheduler/delete-from-ares")
    suspend fun updateInvoicesAmountMismatch() {
        return scheduler.deleteInvoicesNotPresentInPlutus()
    }

    @Get("/balance-amount")
    suspend fun getInvoiceBalanceAmount(@QueryValue("invoiceNumbers") invoiceNumbers: List<String>): List<InvoiceBalanceResponse>? {
        return accUtilService.getInvoiceBalanceAmount(invoiceNumbers)
    }
}
