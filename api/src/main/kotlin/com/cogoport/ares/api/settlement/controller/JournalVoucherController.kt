package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.model.JournalVoucherApproval
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JournalVoucherReject
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.brahma.authentication.Auth
import com.cogoport.brahma.authentication.AuthResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.util.UUID
import javax.validation.Valid

/**
 * Controller to handle all Journal .
 */
@Validated
@Controller("/journal-voucher")
class JournalVoucherController {

    @Inject
    lateinit var journalVoucherService: JournalVoucherService

    @Inject
    lateinit var util: Util
    @Post
    suspend fun createJv(@Body request: JournalVoucherRequest): Response<String> {
        return Response<String>().ok("Request Sent", journalVoucherService.createJournalVoucher(request))
    }

    @Auth
    @Get("/list{?jvListRequest*}")
    suspend fun getJournalVouchers(@Valid jvListRequest: JvListRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<JournalVoucherResponse> {
        jvListRequest.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: jvListRequest.entityCode
        return Response<ResponseList<JournalVoucherResponse>>().ok(journalVoucherService.getJournalVouchersWrapper(jvListRequest))
    }

    @Post("/approve")
    suspend fun approveJv(@Valid @Body request: JournalVoucherApproval): Response<String> {
        return Response<String>().ok("Approved", journalVoucherService.approveJournalVoucher(request))
    }

    @Post("/reject")
    suspend fun rejectJv(@Valid @Body request: JournalVoucherReject): Response<String> {
        return Response<String>().ok("Rejected", journalVoucherService.rejectJournalVoucher(request))
    }

    @Post("/post-to-sage")
    suspend fun postJVToSageUsingJVId(id: Long, performedBy: UUID): Response<String> {
        return Response<String>().ok(
            HttpStatus.OK.name,
            if (journalVoucherService.postJVToSage(id, performedBy)) "Success." else "Failed."
        )
    }
}
