package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.Payment
import java.time.LocalDateTime

interface OnAccountService {
    suspend fun getOnAccountCollections(uploadedDate: LocalDateTime?, entityType: Int?, currencyType: String?): AccountCollectionResponse
    suspend fun upload(): Boolean
    suspend fun createPaymentEntry(receivableRequest: Payment): Payment
    suspend fun updatePaymentEntry(receivableRequest: Payment): Payment?
//    suspend fun updatePostOnPaymentEntry(paymentId: Long): Long?
    suspend fun deletePaymentEntry(paymentId: Long): String?
    suspend fun createBulkPayments(bulkPayment: MutableList<Payment>): BulkPaymentResponse
}
