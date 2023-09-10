package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.migration.service.interfaces.SageService
import com.cogoport.ares.api.payment.service.interfaces.OnAccountService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.DeleteConsolidatedInvoicesReq
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.OrgStatsResponseForCoeFinance
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.PaymentDetailsInfo
import com.cogoport.ares.model.payment.UpdateCSDPaymentRequest
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.BulkUploadRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.OnAccountTotalAmountRequest
import com.cogoport.ares.model.payment.request.SaasInvoiceHookRequest
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.SaasInvoiceHookResponse
import com.cogoport.ares.model.payment.response.UploadSummary
import com.cogoport.ares.model.sage.SageFailedResponse
import com.cogoport.brahma.authentication.Auth
import com.cogoport.brahma.authentication.AuthResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
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

    @Inject lateinit var sageService: SageService

    @Inject
    lateinit var util: Util

    @Auth
    @Get("{?request*}")
    suspend fun getOnAccountCollections(request: AccountCollectionRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): AccountCollectionResponse {
        request.entityType = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityType
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

    @Get("/org-stats-for-coe-finance")
    suspend fun getOrgStatsForCoeFinance(@QueryValue(AresConstants.ORG_ID) orgId: UUID?): OrgStatsResponseForCoeFinance {
        return Response<OrgStatsResponseForCoeFinance>().ok(onAccountService.getOrgStatsForCoeFinance(orgId))
    }

    @Delete("/consolidated")
    suspend fun deleteConsolidatedInvoices(@Body req: DeleteConsolidatedInvoicesReq) {
        onAccountService.deleteConsolidatedInvoices(req)
    }

    @Post("/ap-bulk-upload")
    suspend fun bulkUpload(@Body request: BulkUploadRequest): UploadSummary {
        return onAccountService.onAccountBulkAPPayments(request)
    }
    @Get("/on-account-payment{?request*}")
    suspend fun onAccountTotalAmount(request: OnAccountTotalAmountRequest): MutableList<OnAccountTotalAmountResponse> {
        return Response<MutableList<OnAccountTotalAmountResponse>>().ok(onAccountService.onAccountTotalAmountService(request))
    }

    @Post("/post-to-sage")
    suspend fun postPaymentToSage(id: Long, performedBy: UUID): Response<String> {
        return Response<String>().ok(
            HttpStatus.OK.name,
            if (onAccountService.postPaymentToSage(id, performedBy)) "Success." else "Failed."
        )
    }

    @Post("/bulk-post-to-sage")
    suspend fun bulkPostPaymentToSage(ids: List<Long>, performedBy: UUID) {
        return (onAccountService.bulkPostPaymentToSage(ids, performedBy))
    }

    @Post("/post-from-sage")
    suspend fun postPaymentFromSage(paymentIds: ArrayList<Long>, performedBy: UUID): SageFailedResponse {
        return onAccountService.postPaymentFromSage(paymentIds, performedBy)
    }

    @Post("/cancel-from-sage")
    suspend fun cancelPaymentFromSage(paymentIds: ArrayList<Long>, performedBy: UUID): SageFailedResponse {
        return onAccountService.cancelPaymentFromSage(paymentIds, performedBy)
    }

    @Post("payment/final-post-sage-info")
    suspend fun finalPostSageCheck(paymentNumValue: String, entityCode: Long?, accMode: AccMode): PaymentDetailsInfo? {
        return sageService.getPaymentPostSageInfo(paymentNumValue, entityCode, accMode)
    }

    @Get("/download-sage-platform-report")
    suspend fun downloadSagePlatformReport(startDate: String, endDate: String) {
        return onAccountService.downloadSagePlatformReport(startDate, endDate)
    }

    @Delete("/delete-payments")
    suspend fun deletingApPayments(@Body paymentNumValues: List<String>) {
        return onAccountService.deletingApPayments(paymentNumValues)
    }

    @Post("/update-csd-payment")
    suspend fun updateCSDPayments(@Valid @Body request: UpdateCSDPaymentRequest) {
        return onAccountService.updateCSDPayments(request)
    }

    @Post("/saas-invoice-hook")
    suspend fun saasInvoiceHook(req: SaasInvoiceHookRequest): SaasInvoiceHookResponse {
        return onAccountService.saasInvoiceHook(req)
    }
}
