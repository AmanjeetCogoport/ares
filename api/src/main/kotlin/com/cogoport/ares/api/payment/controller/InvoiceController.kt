package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import com.cogoport.ares.model.payment.response.DueCountResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/invoice")
class InvoiceController {

    @Inject
    lateinit var accUtilService: AccountUtilizationService

    @Inject
    lateinit var accUtilRepo: AccountUtilizationRepository

    @Post("/add-bulk")
    suspend fun addBulkInvoice(@Valid @Body invoiceRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse> {
        return accUtilService.add(invoiceRequestList)
    }

    @Post
    suspend fun addInvoice(@Valid @Body invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {
        return accUtilService.add(invoiceRequest)
    }

    @Get("/due-for-payment")
    suspend fun getKamPaymentCount(@PathVariable("proformaNumbers") proformaNumbers: List<String>,
                                   @PathVariable("pageNumber") pageNumber: Int?,
                                   @PathVariable("pageLimit") pageLimit: Int?): List<DueCountResponse?> {
        return accUtilRepo.getKamPaymentCount(proformaNumbers,pageNumber,pageLimit)
    }
}
