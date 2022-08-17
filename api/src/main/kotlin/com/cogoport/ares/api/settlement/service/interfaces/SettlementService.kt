package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.payment.request.DeleteSettlementRequest
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.EditTdsRequest
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import com.cogoport.ares.model.settlement.SettledInvoice
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.ares.model.settlement.TdsSettlementDocumentRequest
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.ares.model.settlement.request.SettlementDocumentRequest
import java.sql.Timestamp
import java.util.UUID

interface SettlementService {

    suspend fun getDocuments(settlementDocumentRequest: SettlementDocumentRequest): ResponseList<Document>?

    suspend fun getTDSDocuments(request: TdsSettlementDocumentRequest): ResponseList<Document>?

    suspend fun getAccountBalance(summaryRequest: SummaryRequest): SummaryResponse

    suspend fun getHistory(request: SettlementHistoryRequest): ResponseList<HistoryDocument?>

    suspend fun getSettlement(request: SettlementRequest): ResponseList<SettledInvoice?>

    suspend fun check(request: CheckRequest): List<CheckDocument>

    suspend fun editCheck(request: CheckRequest): List<CheckDocument>

    suspend fun settle(request: CheckRequest): List<CheckDocument>

    suspend fun edit(request: CheckRequest): List<CheckDocument>

    suspend fun editTds(request: EditTdsRequest): String

    suspend fun delete(request: DeleteSettlementRequest): String

    suspend fun getOrgSummary(orgId: UUID, startDate: Timestamp?, endDate: Timestamp?): OrgSummaryResponse
}
