package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.SettledInvoice
import com.cogoport.ares.model.settlement.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse

interface SettlementService {

    suspend fun getDocuments(request: SettlementDocumentRequest): ResponseList<Document>?

    suspend fun getAccountBalance(request: SummaryRequest): SummaryResponse

    suspend fun getMatchingBalance(documentIds: List<String>): SummaryResponse

    suspend fun getHistory(request: SettlementHistoryRequest): ResponseList<HistoryDocument?>

    suspend fun getSettlement(request: SettlementRequest): ResponseList<SettledInvoice?>

    suspend fun check(request: CheckRequest): List<CheckDocument>

    suspend fun settle(request: CheckRequest): List<CheckDocument>

    suspend fun edit(request: CheckRequest): List<CheckDocument>
}
