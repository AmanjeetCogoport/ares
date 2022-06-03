package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.Payment

interface OnAccountService {
    suspend fun getOnAccountCollections(request: AccountCollectionRequest): AccountCollectionResponse
    suspend fun createPaymentEntry(receivableRequest: Payment): Payment
    suspend fun updatePaymentEntry(receivableRequest: Payment): Payment?
    suspend fun deletePaymentEntry(paymentId: Long): String?
    suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse
}
