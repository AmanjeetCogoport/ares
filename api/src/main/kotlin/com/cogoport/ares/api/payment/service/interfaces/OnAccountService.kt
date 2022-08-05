package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import java.util.UUID

interface OnAccountService {
    suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse
    suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun updatePaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun deletePaymentEntry(deletePaymentRequest: DeletePaymentRequest): OnAccountApiCommonResponse
    suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse
    suspend fun getOrganizationAccountUtlization(request: LedgerSummaryRequest): List<AccountUtilizationResponse?>
    suspend fun getOrgStats(orgId: UUID?): OrgStatsResponse
}
