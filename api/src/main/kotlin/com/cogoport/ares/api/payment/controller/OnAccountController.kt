package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.OnAccountApiCommonResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Put
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/accounts")
class OnAccountController {

    @Inject
    lateinit var onAccountService: OnAccountService

    @Get("{?request*}")
    suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse {
        return Response<AccountCollectionResponse>().ok(onAccountService.getOnAccountCollections(request))
    }

    @Post
    suspend fun createOnAccountReceivables(@Valid @Body request: Payment): OnAccountApiCommonResponse {
        return Response<OnAccountApiCommonResponse>().ok(onAccountService.createPaymentEntry(request))
    }

    @Put()
    suspend fun updateOnAccountReceivables(@Valid @Body request: Payment): OnAccountApiCommonResponse {
        return Response<OnAccountApiCommonResponse>().ok(onAccountService.updatePaymentEntry(request))
    }

    @Delete
    suspend fun deleteOnAccountReceivables(@QueryValue("paymentId") paymentId: Long): OnAccountApiCommonResponse {
        return Response<OnAccountApiCommonResponse>().ok(onAccountService.deletePaymentEntry(paymentId))
    }

    @Post("/bulk-create")
    suspend fun createBulkOnAccountPayment(@Valid @Body request: MutableList<Payment>): BulkPaymentResponse {
        return Response<BulkPaymentResponse>().ok(onAccountService.createBulkPayments(request))
    }
}
