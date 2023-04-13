package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.entity.JournalCode
import com.cogoport.ares.api.settlement.service.interfaces.JournalCodeService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/journal-code")
class JournalCodeController {

    @Inject
    lateinit var journalCodeService: JournalCodeService

    @Get
    suspend fun getJournalCode(@QueryValue("q") q: String?, @QueryValue("pageLimit") pageLimit: Int? = 10): List<JournalCode> {
        return journalCodeService.getJournalCode(q, pageLimit)
    }
}
