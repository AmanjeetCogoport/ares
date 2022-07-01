package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.SettlementInvoiceRequest
import com.cogoport.ares.model.settlement.InvoiceListResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/settlement")
class SettlementController {

    @Inject
    lateinit var settlementService: SettlementService

    @Get("/invoices{?request*}")
    suspend fun getInvoices(@Valid request: SettlementInvoiceRequest): ResponseList<InvoiceListResponse>? {
        return Response<ResponseList<InvoiceListResponse>?>().ok(settlementService.getInvoices(request))
    }
}
