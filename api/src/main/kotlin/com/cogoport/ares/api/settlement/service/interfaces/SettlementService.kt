package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.settlement.*

interface SettlementService {

    suspend fun getDocuments(request: SettlementDocumentRequest): ResponseList<Document>?
    suspend fun getInvoices(request: SettlementDocumentRequest): ResponseList<Invoice>
    suspend fun getAccountBalance(request: SummaryRequest): SummaryResponse

    suspend fun getMatchingBalance(documentIds: List<String>): SummaryResponse

    suspend fun getHistory(request: SettlementHistoryRequest): ResponseList<HistoryDocument?>

    suspend fun getSettlement(request: SettlementRequest): ResponseList<SettledInvoice?>
    suspend fun knockoff(request: SettlementKnockoffRequest): SettlementKnockoffResponse
}
