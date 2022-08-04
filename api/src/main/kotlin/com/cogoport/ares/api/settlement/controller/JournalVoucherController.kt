package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JvListRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import jakarta.inject.Inject
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
    suspend fun createJv(@Body journalVoucher: JournalVoucher): JournalVoucher {
        return Response<JournalVoucher>().ok(journalVoucherService.createJournalVouchers(journalVoucher))
    }
    @Get("/list{?jvListRequest*}")
    suspend fun getJournalVouchers(@Valid jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse> {
        return Response<ResponseList<JournalVoucherResponse>>().ok(journalVoucherService.getJournalVouchers(jvListRequest))
    }
}
