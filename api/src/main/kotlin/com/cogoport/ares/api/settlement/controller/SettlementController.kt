package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.Invoice
import com.cogoport.ares.model.settlement.SettledInvoice
import com.cogoport.ares.model.settlement.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import com.cogoport.ares.model.settlement.SettlementKnockoffResponse
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

/**
 * Controller to handle all input requests for settlement API.
 */
@Validated
@Controller("/settlement")
class SettlementController {

    @Inject
    lateinit var settlementService: SettlementService

    @Get("/documents{?request*}")
    suspend fun getDocuments(@Valid request: SettlementDocumentRequest): ResponseList<Document>? {
        return Response<ResponseList<Document>?>().ok(settlementService.getDocuments(request))
    }

    @Get("/invoices{?request*}")
    suspend fun getInvoices(@Valid request: SettlementDocumentRequest): ResponseList<Invoice>? {
        return Response<ResponseList<Invoice>?>().ok(settlementService.getInvoices(request))
    }

    @Get("/account-balance{?request*}")
    suspend fun getAccountBalance(@Valid request: SummaryRequest): SummaryResponse {
        return Response<SummaryResponse>().ok(settlementService.getAccountBalance(request))
    }

    @Get("/matching-balance")
    suspend fun getMatchingBalance(@QueryValue("documentIds") documentIds: List<String>): SummaryResponse {
        return Response<SummaryResponse>().ok(settlementService.getMatchingBalance(documentIds))
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
        return Response<SettlementKnockoffResponse>().ok(SettlementKnockoffResponse())
    }

    @Post("/check")
    suspend fun check(@Body request: CheckRequest): List<CheckDocument> {
        return Response<List<CheckDocument>>().ok(settlementService.check(request))
    }

    @Post("/settle")
    suspend fun settle(@Body request: CheckRequest): List<CheckDocument> {
        return Response<List<CheckDocument>>().ok(settlementService.settle(request))
    }

    @Post("/edit")
    suspend fun edit(@Body request: CheckRequest): List<CheckDocument> {
        return Response<List<CheckDocument>>().ok(settlementService.edit(request))
    }
}
