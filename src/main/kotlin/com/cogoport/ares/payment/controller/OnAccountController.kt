package com.cogoport.ares.payment.controller

import com.cogoport.ares.payment.model.AccountCollectionResponse
import com.cogoport.ares.payment.model.Payment
import com.cogoport.ares.payment.service.interfaces.OnAccountService
import io.micronaut.context.annotation.Parameter
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.time.LocalDateTime
import javax.validation.Valid

@Validated
@Controller("/receivables")
class OnAccountController {

    @Inject
    lateinit var onAccountService: OnAccountService

    @Get("/collections")
    suspend fun getOnAccountCollections(
        @QueryValue("uploadedDate") uploadedDate: LocalDateTime?,
        @QueryValue("entityType") entityType: Int?,
        @QueryValue("currencyType") currencyType: String?
    ): AccountCollectionResponse {
        return onAccountService.getOnAccountCollections(LocalDateTime.now(), entityType, currencyType)
    }
    @Post("/upload/{userId}", consumes = [MediaType.MULTIPART_FORM_DATA], produces = [MediaType.TEXT_PLAIN])
    suspend fun upload(@Parameter("file") file: StreamingFileUpload, @PathVariable("userId") userId: String): Boolean {
        return onAccountService.upload()
    }
    @Post("/collection/create")
    suspend fun createOnAccountReceivables(@Valid @Body request: Payment): Payment {
        return onAccountService.createReceivables(request)
    }

}