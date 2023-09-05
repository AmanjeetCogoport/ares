package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.PushAccountUtilizationRequest
import com.cogoport.ares.model.common.DeleteConsolidatedInvoicesReq
import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.OrgStatsResponseForCoeFinance
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.BulkUploadRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.OnAccountTotalAmountRequest
import com.cogoport.ares.model.payment.response.ARLedgerResponse
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.SaasInvoiceHookResponse
import com.cogoport.ares.model.payment.response.UploadSummary
import com.cogoport.ares.model.sage.SageFailedResponse
import com.cogoport.ares.model.settlement.PostPaymentToSage
import com.cogoport.hades.model.incident.request.SaasUTRUploadRequest
import java.util.UUID

interface OnAccountService {
    suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse
    suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun updatePaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun deletePaymentEntry(deletePaymentRequest: DeletePaymentRequest): OnAccountApiCommonResponse
    suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse
    suspend fun getOrganizationAccountUtlization(request: LedgerSummaryRequest): List<AccountUtilizationResponse?>
    suspend fun getOrgStats(orgId: UUID?): OrgStatsResponse
    suspend fun getOrgStatsForCoeFinance(orgId: UUID?): OrgStatsResponseForCoeFinance
    suspend fun getDataAccUtilization(request: PushAccountUtilizationRequest): List<AccountUtilization>
    suspend fun deleteConsolidatedInvoices(req: DeleteConsolidatedInvoicesReq)
    suspend fun onAccountBulkAPPayments(req: BulkUploadRequest): UploadSummary
    suspend fun onAccountTotalAmountService(req: OnAccountTotalAmountRequest): MutableList<OnAccountTotalAmountResponse>
    suspend fun postPaymentToSage(paymentId: Long, performedBy: UUID): Boolean
    suspend fun bulkPostPaymentToSage(paymentId: List<Long>, performedBy: UUID)
    suspend fun postPaymentFromSage(paymentIds: ArrayList<Long>, performedBy: UUID): SageFailedResponse
    suspend fun cancelPaymentFromSage(paymentIds: ArrayList<Long>, performedBy: UUID): SageFailedResponse
    suspend fun createPaymentEntryAndReturnUtr(request: Payment)
    suspend fun directFinalPostToSage(req: PostPaymentToSage)
    suspend fun bulkUpdatePaymentAndPostOnSage(req: PostPaymentToSage)

    suspend fun downloadSagePlatformReport(startDate: String, endDate: String)

    suspend fun deletingApPayments(paymentNumValues: List<String>)
    suspend fun getARLedgerOrganizationAndEntityWise(req: LedgerSummaryRequest): List<ARLedgerResponse>

    suspend fun saasInvoiceHook(req: SaasUTRUploadRequest): SaasInvoiceHookResponse
}
