package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.Payment
import java.time.LocalDateTime

interface OnAccountService {
    suspend fun getOnAccountCollections(uploadedDate: LocalDateTime?, entityType: Int?, currencyType: String?): AccountCollectionResponse
    suspend fun createPaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun updatePaymentEntry(receivableRequest: Payment): OnAccountApiCommonResponse
    suspend fun deletePaymentEntry(paymentId: Long): OnAccountApiCommonResponse
    suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse
}
