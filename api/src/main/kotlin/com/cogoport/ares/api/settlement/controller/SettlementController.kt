package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.payment.service.interfaces.SettlementService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.OverallStatsRequest
import com.cogoport.ares.model.payment.OverallStatsResponse
import com.cogoport.ares.model.payment.SettlementInvoiceRequest
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

    @Get("/invoices")
    suspend fun getInvoices(@Valid request: SettlementInvoiceRequest): ResponseList<AccountUtilizationResponse>? {
        return Response<ResponseList<AccountUtilizationResponse>?>().ok(settlementService.getInvoices(request))
    }
}
