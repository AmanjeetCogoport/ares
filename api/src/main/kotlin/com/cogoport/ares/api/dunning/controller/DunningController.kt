package com.cogoport.ares.api.dunning.controller

import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import javax.validation.Valid

@Controller("/dunning")
class DunningController (
    private val dunningService: DunningService
        ){

    @Get("/master-list{?request*}")
    suspend fun listMasterException(@Valid  request: ): List<CreateInvoiceResponse> {
        return dunningService.listMasterException(request)
    }
}
