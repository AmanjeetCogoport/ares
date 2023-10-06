package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.common.models.FindRecordByDocumentNo
import com.cogoport.ares.api.common.service.implementation.Scheduler
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.entity.SettlementListDoc
import com.cogoport.ares.api.settlement.service.interfaces.CpSettlementService
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.settlement.service.interfaces.TaggedSettlementService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.DeleteSettlementRequest
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
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
import com.cogoport.ares.model.settlement.SettlementInvoiceRequest
import com.cogoport.ares.model.settlement.SettlementInvoiceResponse
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import com.cogoport.ares.model.settlement.SettlementKnockoffResponse
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.ares.model.settlement.TdsSettlementDocumentRequest
import com.cogoport.ares.model.settlement.request.AutoKnockOffRequest
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.ares.model.settlement.request.OrgSummaryRequest
import com.cogoport.ares.model.settlement.request.RejectSettleApproval
import com.cogoport.ares.model.settlement.request.SettlementDocumentRequest
import com.cogoport.brahma.authentication.Auth
import com.cogoport.brahma.authentication.AuthResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
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
    lateinit var scheduler: Scheduler

    @Inject
    lateinit var cpSettlementService: CpSettlementService
    @Inject
    lateinit var util: Util

    @Inject
    lateinit var taggedSettlementService: TaggedSettlementService

    @Auth
    @Get("/documents{?request*}")
    suspend fun getDocuments(@Valid request: SettlementDocumentRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<Document>? {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
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
    @Auth
    @Get("/account-balance{?request*}")
    suspend fun getAccountBalance(@Valid request: SummaryRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): SummaryResponse {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        return Response<SummaryResponse>().ok(settlementService.getAccountBalance(request))
    }

    @Auth
    @Get("/history{?request*}")
    suspend fun getHistory(@Valid request: SettlementHistoryRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<HistoryDocument?> {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        request.sortBy = request.sortBy ?: "transactionDate"
        request.sortType = request.sortType ?: "Desc"
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
        return Response<List<CheckDocument>>().ok(settlementService.settleWrapper(request))
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
    suspend fun settleWithSourceIdAndDestinationId(@Valid @Body autoKnockOffRequest: AutoKnockOffRequest): List<CheckDocument>? {
        return settlementService.settleWithSourceIdAndDestinationId(autoKnockOffRequest)
    }

    @Post("/send-utilization-to-debit")
    suspend fun sendPaymentDataToDebit(@Valid @Body request: FindRecordByDocumentNo) {
        return settlementService.sendInvoiceDataToDebitConsumption(request)
    }

    @Post("/unfreeze-credit-consumption")
    suspend fun unfreezeCreditConsumption(@Valid @Body request: Settlement) {
        return settlementService.sendKnockOffDataToCreditConsumption(request)
    }

    @Post("/settle-tagged-invoice-payment")
    suspend fun settleOnAccountTaggedInvoicePayment(@Body req: OnAccountPaymentRequest) {
        return taggedSettlementService.settleOnAccountInvoicePayment(req)
    }

    @Post("/bulk-matching-on-sage")
    suspend fun bulkMatchingSettlementOnSage(settlementIds: List<Long>, performedBy: UUID) {
        return settlementService.bulkMatchingSettlementOnSage(settlementIds, performedBy)
    }

    @Post("/cron-bulk-matching-on-sage")
    suspend fun cronBulkMatchingSettlementOnSage() {
        return scheduler.bulkMatchingSettlement()
    }

    @Post("/matching-on-sage")
    suspend fun matchingOnSage(settlementIds: List<Long>, performedBy: UUID): SageFailedResponse {
        return settlementService.matchingOnSage(settlementIds, performedBy)
    }

    @Post("/email-notification-settlement-matching-failed")
    suspend fun cronSettlementMatchingFailedOnSageEmail(): HttpResponse<Map<String, String>> {
        scheduler.settlementMatchingFailedOnSageEmail()
        return HttpResponse.ok(mapOf("status" to "ok"))
    }

    @Auth
    @Get("/list{?request*}")
    suspend fun getSettlementList(@Valid request: SettlementHistoryRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<SettlementListDoc?> {
        request.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: request.entityCode
        request.sortBy = request.sortBy ?: "settlementDate"
        request.sortType = request.sortType ?: "Desc"
        return Response<ResponseList<SettlementListDoc?>>().ok(settlementService.getSettlementList(request))
    }
}
