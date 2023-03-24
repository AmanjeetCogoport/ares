package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.OpenSearchListRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.model.payment.ServiceType
import java.util.UUID

interface OpenSearchService {
    suspend fun pushOutstandingData(request: OpenSearchRequest)
    suspend fun pushOutstandingListData(request: OpenSearchListRequest)
    suspend fun paymentDocumentStatusMigration()

    suspend fun generateOutstandingData(
        searchKey: String,
        entityCode: Int?,
        defaultersOrgIds: List<UUID>?,
        dashboardCurrency: String?
    )
}
