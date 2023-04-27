package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.OpenSearchListRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest

interface OpenSearchService {
    suspend fun pushOutstandingData(request: OpenSearchRequest)
    suspend fun pushOutstandingListData(request: OpenSearchListRequest)
    suspend fun paymentDocumentStatusMigration()

    suspend fun fetchPaymentFromOpenSearch(id: Long): com.cogoport.ares.model.payment.Payment
}
