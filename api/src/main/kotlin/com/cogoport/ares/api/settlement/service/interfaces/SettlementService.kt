package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.common.models.FindRecordByDocumentNo
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.entity.SettlementListDoc
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.DeleteSettlementRequest
import com.cogoport.ares.model.sage.SageFailedResponse
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckResponse
import com.cogoport.ares.model.settlement.CreateIncidentRequest
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
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.ares.model.settlement.request.OrgSummaryRequest
import com.cogoport.ares.model.settlement.request.RejectSettleApproval
import com.cogoport.ares.model.settlement.request.SettlementDocumentRequest
import java.util.UUID

interface SettlementService {

    suspend fun getDocuments(settlementDocumentRequest: SettlementDocumentRequest): ResponseList<Document>?

    suspend fun getTDSDocuments(request: TdsSettlementDocumentRequest): ResponseList<Document>?

    suspend fun getAccountBalance(summaryRequest: SummaryRequest): SummaryResponse

    suspend fun getHistory(request: SettlementHistoryRequest): ResponseList<HistoryDocument?>

    suspend fun getSettlement(request: SettlementRequest): ResponseList<SettledInvoice?>

    suspend fun check(request: CheckRequest): CheckResponse

    suspend fun editCheck(request: CheckRequest): CheckResponse

    suspend fun settleWrapper(request: CheckRequest, isAutoKnockOff: Boolean = false): List<CheckDocument>

    suspend fun settle(request: CheckRequest, isAutoKnockOff: Boolean = false): List<CheckDocument>

    suspend fun edit(request: CheckRequest): List<CheckDocument>

    suspend fun editTds(request: EditTdsRequest): String

    suspend fun delete(request: DeleteSettlementRequest): String

    suspend fun sendForApproval(request: CreateIncidentRequest): String

    suspend fun reject(request: RejectSettleApproval): String

    suspend fun getOrgSummary(request: OrgSummaryRequest): OrgSummaryResponse

    suspend fun settleWithSourceIdAndDestinationId(autoKnockOffRequest: AutoKnockOffRequest): List<CheckDocument>?

    suspend fun sendKnockOffDataToCreditConsumption(request: Settlement)

    suspend fun sendInvoiceDataToDebitConsumption(request: FindRecordByDocumentNo)

    suspend fun matchingSettlementOnSage(settlementId: Long, performedBy: UUID): Boolean

    suspend fun bulkMatchingSettlementOnSage(settlementIds: List<Long>, performedBy: UUID)

    suspend fun matchingOnSage(settlementIds: List<Long>, performedBy: UUID): SageFailedResponse

    suspend fun sendEmailSettlementsMatchingFailed(url: String)

    suspend fun getSettlementList(request: SettlementHistoryRequest): ResponseList<SettlementListDoc?>
}
