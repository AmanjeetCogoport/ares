package com.cogoport.ares.payment.service.interfaces

import com.cogoport.ares.payment.model.AccountCollectionResponse
import com.cogoport.ares.payment.model.Payment
import java.time.LocalDateTime

interface OnAccountService {
    suspend fun getOnAccountCollections(uploadedDate: LocalDateTime?, entityType: Int?, currencyType: String?): AccountCollectionResponse

    suspend fun upload(): Boolean

    suspend fun createReceivables(receivableRequest: Payment): Payment
}
