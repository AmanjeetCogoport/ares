package com.cogoport.ares.api.dunning.controller

import com.cogoport.ares.api.dunning.service.interfaces.EmailService
import com.cogoport.ares.common.models.Response
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import javax.validation.Valid

@Validated
@Controller("/email")
class EmailController(
        private val EmailService: EmailService,
){
    @Post("/send-email")
    suspend fun sendEmail(
            @Valid @Body invoiceId: Long
    ) {
        EmailService.sendEmailForIrnGeneration(invoiceId)
    }


}