package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.model.settlement.JournalVoucher
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated

/**
 * Controller to handle all Jo=urnal .
 */
@Validated
@Controller("/journal-voucher")
class JournalVoucherController {

    @Post
    suspend fun createJv(@Body journalVoucher: JournalVoucher) {

    }

    @Get
    suspend fun getJv() {

    }
}