package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.payment.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse

interface SettlementService {

    suspend fun getDocuments(request: SettlementDocumentRequest): ResponseList<Document>?

    suspend fun getAccountBalance(request: SummaryRequest): SummaryResponse

    suspend fun getMatchingBalance(documentIds: List<String>): SummaryResponse
}
