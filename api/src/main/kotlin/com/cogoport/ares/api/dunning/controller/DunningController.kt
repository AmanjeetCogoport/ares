package com.cogoport.ares.api.dunning.controller

import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.dunning.request.CreditControllerRequest
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest
import com.cogoport.brahma.hashids.Hashids
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/dunning")
class DunningController {
    @Inject
    lateinit var dunningService: DunningService

    @Post("/credit-controller")
    suspend fun createCreditController(
        @Valid @Body
        creditControllerRequest: CreditControllerRequest
    ): String {
        return Response<String>().ok(Hashids.encode(dunningService.createCreditController(creditControllerRequest)))
    }

    @Put("/credit-controller")
    suspend fun updateCreditController(
        @Valid @Body
        updateCreditControllerRequest: UpdateCreditControllerRequest
    ): String {
        return Response<String>().ok(Hashids.encode(dunningService.updateCreditController(updateCreditControllerRequest)))
    }
}
