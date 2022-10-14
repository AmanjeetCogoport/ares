package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.PushAccountUtilizationRequest
import com.cogoport.ares.model.common.DeleteConsolidatedInvoicesReq
import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.request.*
import com.cogoport.ares.model.payment.response.*
import java.util.UUID

interface OnAccountService {
    suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse
    suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun updatePaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun deletePaymentEntry(deletePaymentRequest: DeletePaymentRequest): OnAccountApiCommonResponse
    suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse
    suspend fun getOrganizationAccountUtlization(request: LedgerSummaryRequest): List<AccountUtilizationResponse?>
    suspend fun getOrgStats(orgId: UUID?): OrgStatsResponse
    suspend fun getDataAccUtilization(request: PushAccountUtilizationRequest): List<AccountUtilization>
    suspend fun deleteConsolidatedInvoices(req: DeleteConsolidatedInvoicesReq)
    suspend fun onAccountBulkAPPayments(req: BulkUploadRequest): UploadSummary

    suspend fun onAccountTotalAmountService(req: OnAccountTotalAmountRequest) : MutableList<OnAccountTotalAmountResponse>
}
