package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.DeleteConsolidatedInvoicesReq
import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.BulkUploadRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.response.UploadSummary
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.util.UUID
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
    suspend fun deleteOnAccountReceivables(@Body deletePaymentRequest: DeletePaymentRequest): OnAccountApiCommonResponse {
        return Response<OnAccountApiCommonResponse>().ok(onAccountService.deletePaymentEntry(deletePaymentRequest))
    }

    @Post("/bulk-create")
    suspend fun createBulkOnAccountPayment(@Valid @Body request: MutableList<Payment>): BulkPaymentResponse {
        return Response<BulkPaymentResponse>().ok(onAccountService.createBulkPayments(request))
    }

    @Get("/ledger-summary{?request*}")
    suspend fun getOrganizationAccountUtilization(request: LedgerSummaryRequest): List<AccountUtilizationResponse?> {
        return Response<List<AccountUtilizationResponse?>>().ok(onAccountService.getOrganizationAccountUtlization(request))
    }

    @Get("/org-stats")
    suspend fun getOrgStats(@QueryValue(AresConstants.ORG_ID) orgId: UUID?): OrgStatsResponse {
        return Response<OrgStatsResponse>().ok(onAccountService.getOrgStats(orgId))
    }

    @Delete("/consolidated")
    suspend fun deleteConsolidatedInvoices(@Body req: DeleteConsolidatedInvoicesReq) {
        onAccountService.deleteConsolidatedInvoices(req)
    }

    @Post("/ap-bulk-upload")
    suspend fun bulkUpload(@Body request: BulkUploadRequest): UploadSummary {
        return onAccountService.onAccountBulkAPPayments(request)
    }
}
