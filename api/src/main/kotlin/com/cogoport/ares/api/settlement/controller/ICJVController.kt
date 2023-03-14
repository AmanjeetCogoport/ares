package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.model.ICJVUpdateRequest
import com.cogoport.ares.api.settlement.service.interfaces.ICJVService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentICJVRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/icjv")
class ICJVController {

    @Inject
    lateinit var icjvService: ICJVService

    @Post
    suspend fun createJv(@Body request: ParentICJVRequest): Response<String> {
        return Response<String>().ok("Request Sent", icjvService.createICJV(request))
    }

    @Get("/list{?jvListRequest*}")
    suspend fun getJournalVouchers(@Valid jvListRequest: JvListRequest): ResponseList<ParentJournalVoucherResponse> {
        return Response<ResponseList<ParentJournalVoucherResponse>>().ok(icjvService.getJournalVouchers(jvListRequest))
    }

    @Get
    suspend fun getJournalVoucherByParentJVId(@QueryValue("parentId") parentId: String): List<JournalVoucherResponse> {
        return icjvService.getJournalVoucherByParentJVId(parentId)
    }

    @Post("/update")
    suspend fun updateICJV(@Valid @Body request: ICJVUpdateRequest): String {
        return icjvService.updateICJV(request)
    }
}
