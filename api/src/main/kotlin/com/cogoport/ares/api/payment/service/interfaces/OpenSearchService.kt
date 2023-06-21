package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.OpenSearchListRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.brahma.rabbitmq.model.RabbitmqEventLogDocument

interface OpenSearchService {
    suspend fun pushOutstandingData(request: OpenSearchRequest)
    suspend fun pushOutstandingListData(request: OpenSearchListRequest)

    suspend fun pushEventLogsToOpenSearch(rabbitmqEventLogDocument: RabbitmqEventLogDocument)
}
