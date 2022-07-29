package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.settlement.service.interfaces.CpSettlementService
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.EditTdsRequest
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import com.cogoport.ares.model.settlement.SettledInvoice
import com.cogoport.ares.model.settlement.SettlementDocumentRequest
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
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.sql.Timestamp
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
    suspend fun check(@Body request: CheckRequest): List<CheckDocument> {
        return Response<List<CheckDocument>>().ok(settlementService.check(request))
    }

    @Post("/edit-check")
    suspend fun editCheck(@Body request: CheckRequest): List<CheckDocument> {
        return Response<List<CheckDocument>>().ok(settlementService.editCheck(request))
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
    suspend fun editTds(@Valid @Body request: EditTdsRequest): Long {
        return Response<Long>().ok(settlementService.editTds(request))
    }

    @Delete
    suspend fun delete(@QueryValue documentNo: Long, @QueryValue settlementType: SettlementType): Long {
        return Response<Long>().ok(settlementService.delete(documentNo, settlementType))
    }

    @Get("/org-summary")
    suspend fun getOrgSummary(
        @QueryValue(AresModelConstants.ORG_ID) orgId: UUID,
        @QueryValue(AresModelConstants.START_DATE) startDate: Timestamp? = null,
        @QueryValue(AresModelConstants.END_DATE) endDate: Timestamp? = null
    ): OrgSummaryResponse {
        return Response<OrgSummaryResponse>().ok(
            settlementService.getOrgSummary(
                orgId, startDate, endDate
            )
        )
    }
}
