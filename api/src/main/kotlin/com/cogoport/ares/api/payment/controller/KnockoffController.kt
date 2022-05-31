package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.model.AccountPayableFileResponse
import com.cogoport.ares.api.payment.model.AccountPayablesFile
import com.cogoport.ares.api.payment.service.interfaces.KnockoffService
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
    suspend fun addInvoice(@Valid @Body payableList: List<AccountPayablesFile>): MutableList<AccountPayableFileResponse> {
        return knockoffService.uploadBillPayment(payableList)
    }
}