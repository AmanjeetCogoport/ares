package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.service.interfaces.CpSettlementService
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.DeleteSettlementRequest
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckResponse
import com.cogoport.ares.model.settlement.CreateIncidentRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.EditTdsRequest
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import com.cogoport.ares.model.settlement.SettledInvoice
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementInvoiceRequest
import com.cogoport.ares.model.settlement.SettlementInvoiceResponse
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import com.cogoport.ares.model.settlement.SettlementKnockoffResponse
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.ares.model.settlement.TdsSettlementDocumentRequest
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.ares.model.settlement.request.OrgSummaryRequest
import com.cogoport.ares.model.settlement.request.RejectSettleApproval
import com.cogoport.ares.model.settlement.request.SettlementDocumentRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.util.UUID
import javax.validation.Valid

/**
 * Controller to handle all input requests for settlement API.
 */
@Validated
@Controller("/settlement")
class SettlementController {

    @Inject
    lateinit var settlementService: SettlementService

    @Inject
    lateinit var cpSettlementService: CpSettlementService

    @Get("/documents{?request*}")
    suspend fun getDocuments(@Valid request: SettlementDocumentRequest): ResponseList<Document>? {
        return Response<ResponseList<Document>?>().ok(settlementService.getDocuments(request))
    }

    @Get("/tds/documents{?request*}")
    suspend fun getTDSDocuments(@Valid request: TdsSettlementDocumentRequest): ResponseList<Document>? {
        return Response<ResponseList<Document>?>().ok(settlementService.getTDSDocuments(request))
    }

    /**
     * API to be consumed at CP/LSP side.
     * @param : SettlementDocumentRequest
     * @return: List
     */
    @Get("/invoices{?request*}")
    suspend fun getInvoices(@Valid request: SettlementInvoiceRequest): ResponseList<SettlementInvoiceResponse>? {
        return Response<ResponseList<SettlementInvoiceResponse>?>().ok(cpSettlementService.getInvoices(request))
    }

    @Get("/account-balance{?request*}")
    suspend fun getAccountBalance(@Valid request: SummaryRequest): SummaryResponse {
        return Response<SummaryResponse>().ok(settlementService.getAccountBalance(request))
    }

    @Get("/history{?request*}")
    suspend fun getHistory(@Valid request: SettlementHistoryRequest): ResponseList<HistoryDocument?> {
        return Response<ResponseList<HistoryDocument?>>().ok(settlementService.getHistory(request))
    }

    @Get("{?request*}")
    suspend fun getSettlement(@Valid request: SettlementRequest): ResponseList<SettledInvoice?> {
        return Response<ResponseList<SettledInvoice?>>().ok(settlementService.getSettlement(request))
    }

    @Post("/knockoff")
    suspend fun knockoff(@Valid @Body request: SettlementKnockoffRequest): SettlementKnockoffResponse {
        return Response<SettlementKnockoffResponse>().ok(cpSettlementService.knockoff(request))
    }

    @Post("/check")
    suspend fun check(@Body request: CheckRequest): CheckResponse {
        return Response<CheckResponse>().ok(settlementService.check(request))
    }

    @Post("/edit-check")
    suspend fun editCheck(@Body request: CheckRequest): CheckResponse {
        return Response<CheckResponse>().ok(settlementService.editCheck(request))
    }

    @Post("/settle")
    suspend fun settle(@Body request: CheckRequest): List<CheckDocument> {
        return Response<List<CheckDocument>>().ok(settlementService.settle(request))
    }

    @Post("/edit")
    suspend fun edit(@Body request: CheckRequest): List<CheckDocument> {
        return Response<List<CheckDocument>>().ok(settlementService.edit(request))
    }

    @Post("/edit-tds")
    suspend fun editTds(@Valid @Body request: EditTdsRequest): String {
        return Response<String>().ok(settlementService.editTds(request))
    }

    @Post("/send-for-approval")
    suspend fun sendForApproval(@Valid @Body request: CreateIncidentRequest): Response<String> {
        return Response<String>().ok("Sent For Approval", settlementService.sendForApproval(request))
    }

    @Post("/reject")
    suspend fun reject(@Valid @Body request: RejectSettleApproval): Response<String> {
        return Response<String>().ok("Rejected", settlementService.reject(request))
    }

    @Delete
    suspend fun delete(
        @QueryValue("documentNo") documentNo: String,
        @QueryValue("settlementType") settlementType: SettlementType,
        @QueryValue("deletedBy") deletedBy: UUID,
        @QueryValue("deletedByUserType") deletedByUserType: String?
    ): String {
        return Response<String>().ok(
            settlementService.delete(
                DeleteSettlementRequest(
                    documentNo = documentNo,
                    settlementType = settlementType,
                    deletedBy = deletedBy,
                    deletedByUserType = deletedByUserType
                )
            )
        )
    }

    @Get("/org-summary{?request*}")
    suspend fun getOrgSummary(@Valid request: OrgSummaryRequest): OrgSummaryResponse {
        return Response<OrgSummaryResponse>().ok(
            settlementService.getOrgSummary(request)
        )
    }

    @Post("/settle-with-source-and-destination-id")
    suspend fun settleWithSourceIdAndDestinationId(@QueryValue("sourceId") sourceId: String, @QueryValue("destinationId") destinationId: String, @QueryValue("sourceType") sourceType: SettlementType, @QueryValue("destinationType") destinationType: SettlementType): List<CheckDocument>? {
        return settlementService.settleWithSourceIdAndDestinationId(sourceId, destinationId, sourceType, destinationType)
    }
}
