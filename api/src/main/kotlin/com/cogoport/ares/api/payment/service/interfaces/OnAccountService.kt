package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.DeletePaymentRequest
import com.cogoport.ares.model.payment.LedgerSummaryRequest
import com.cogoport.ares.model.payment.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.Payment

interface OnAccountService {
    suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse
    suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun updatePaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun deletePaymentEntry(deletePaymentRequest: DeletePaymentRequest): OnAccountApiCommonResponse
    suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse
    suspend fun getOrganizationAccountUtlization(request: LedgerSummaryRequest): List<AccountUtilizationResponse?>
}
