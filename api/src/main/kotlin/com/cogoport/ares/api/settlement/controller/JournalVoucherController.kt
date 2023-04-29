package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.model.settlement.JvLineItemResponse
import com.cogoport.brahma.authentication.Auth
import com.cogoport.brahma.authentication.AuthResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

/**
 * Controller to handle all Journal Voucher Line Item.
 */
@Validated
@Controller("/journal-voucher")
class JournalVoucherController {

    @Inject
    lateinit var journalVoucherService: JournalVoucherService

    @Inject
    lateinit var util: Util

    @Auth
    @Get("/jv-line-items-list")
    suspend fun getJVLineItems(@QueryValue("parentJVId") parentJVId: String, user: AuthResponse?, httpRequest: HttpRequest<*>): MutableList<JvLineItemResponse> {
        return journalVoucherService.getJVLineItems(parentJVId)
    }
}
