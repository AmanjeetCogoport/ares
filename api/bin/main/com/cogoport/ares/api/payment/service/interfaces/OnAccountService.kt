package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.Payment
import java.time.LocalDateTime

interface OnAccountService {
    suspend fun getOnAccountCollections(uploadedDate: LocalDateTime?, entityType: Int?, currencyType: String?): AccountCollectionResponse

    suspend fun upload(): Boolean

    suspend fun createReceivables(receivableRequest: Payment): Payment
}
