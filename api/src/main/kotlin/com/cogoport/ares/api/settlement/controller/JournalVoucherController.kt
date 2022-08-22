package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.settlement.model.JournalVoucherApproval
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
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

    @Post
    suspend fun createJv(@Body request: JournalVoucherRequest): Response<String> {
        return Response<String>().ok("Created Successfully",journalVoucherService.createJournalVoucher(request))
    }

    @Get("/list{?jvListRequest*}")
    suspend fun getJournalVouchers(@Valid jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse> {
        return Response<ResponseList<JournalVoucherResponse>>().ok(journalVoucherService.getJournalVouchers(jvListRequest))
    }

    @Post("/approved")
    suspend fun approveJv(@Valid @Body request: JournalVoucherApproval): Response<String> {
        return Response<String>().ok("Successful",journalVoucherService.approveJournalVoucher(request))
    }

    @Post("/rejected")
    suspend fun rejectJv(id: Long, performedBy: UUID?) {
        journalVoucherService.rejectJournalVoucher(id, performedBy)
    }
}
