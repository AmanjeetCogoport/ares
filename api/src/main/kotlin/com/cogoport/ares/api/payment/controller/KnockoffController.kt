package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
import com.cogoport.ares.model.payment.AccountPayableFileResponse
import com.cogoport.ares.model.payment.AccountPayablesFile
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/knockoff")
class KnockoffController {

    @Inject
    lateinit var knockoffService: KnockoffService

    @Post("/payables")
    suspend fun knockOffPayables(@Valid @Body payableList: List<AccountPayablesFile>): MutableList<AccountPayableFileResponse> {
        return knockoffService.uploadBillPayment(payableList)
    }
}
