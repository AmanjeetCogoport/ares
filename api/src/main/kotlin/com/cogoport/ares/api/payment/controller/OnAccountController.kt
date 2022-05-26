package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import io.micronaut.context.annotation.Parameter
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Body
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

    @Get("/payments")
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
    @Post("/payment")
    suspend fun createOnAccountReceivables(@Valid @Body request: Payment): Payment {
        return onAccountService.createPaymentEntry(request)
    }

//    @Post("/payment/post/{paymentId}")
//    suspend fun markPostedOnPayment(@PathVariable("paymentId") paymentId: Long): Long? {
//        return onAccountService.updatePostOnPaymentEntry(paymentId)
//    }
    @Post("/payment/update")
    suspend fun updateOnAccountReceivables(@Valid @Body request: Payment): Payment? {
        return onAccountService.updatePaymentEntry(request)
    }

    @Delete("/payment/{paymentId}")
    suspend fun deleteOnAccountReceivables(@PathVariable("paymentId") paymentId: Long): String? {
        return onAccountService.deletePaymentEntry(paymentId)
    }
}
