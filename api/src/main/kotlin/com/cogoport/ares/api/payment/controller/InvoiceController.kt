package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.service.interfaces.AccountUtilizationService
import com.cogoport.ares.api.payment.service.interfaces.GetInformation
import com.cogoport.ares.model.common.TestModel
import com.cogoport.ares.model.payment.event.DeleteInvoiceRequest
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
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
    lateinit var getinformation: GetInformation

    @Post("/add-bulk")
    suspend fun addBulkInvoice(@Valid @Body invoiceRequestList: List<AccUtilizationRequest>): List<CreateInvoiceResponse> {
        return accUtilService.add(invoiceRequestList)
    }

    @Post
    suspend fun addInvoice(@Valid @Body invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse {
        return accUtilService.add(invoiceRequest)
    }
    @Get("/hello{?request*}")
    suspend fun getBillingDetails(@Valid request: TestModel): String {
        return "successs1"
    }

    @Get("/get-curr-outstanding-on-day-of-computation")
    suspend fun getCurrOutstanding(@QueryValue req: List<Long>): Long {
        return getinformation.getCurrOutstanding(req)
    }

    @Delete
    suspend fun deleteInvoice(@Valid @Body deleteRequest: DeleteInvoiceRequest): Boolean {
        return accUtilService.delete(deleteRequest)
    }
}
