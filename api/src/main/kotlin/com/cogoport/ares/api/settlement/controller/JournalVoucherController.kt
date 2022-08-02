package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucher
import com.cogoport.ares.model.settlement.request.JvListRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import javax.validation.Valid

/**
 * Controller to handle all Journal .
 */
@Validated
@Controller("/journal-voucher")
class JournalVoucherController {

    @Post
    suspend fun createJv(@Body journalVoucher: JournalVoucher) {

    }

    @Get("/list")
    suspend fun getJournalVouchers(@Valid jvListRequest: JvListRequest): ResponseList<JournalVoucher> {
        TODO()
    }
}