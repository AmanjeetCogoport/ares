package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.service.interfaces.DefaultedBusinessPartnersService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.DefaultedBusinessPartnerRequest
import com.cogoport.ares.model.payment.request.ListDefaultedBusinessPartnersRequest
import com.cogoport.ares.model.payment.response.DefaultedBusinessPartnersResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/bpr")
class DefaultedBusinessPartnersController {

    @Inject
    lateinit var bprService: DefaultedBusinessPartnersService

    @Post
    suspend fun addOrganization(@Valid @Body request: DefaultedBusinessPartnerRequest): Response<Long> {
        return Response<Long>().ok("Saved", bprService.add(request))
    }

    @Delete("/delete/{id}")
    suspend fun remove(@PathVariable("id") id: Long): Response<Long> {
        return Response<Long>().ok("Deleted", bprService.delete(id))
    }

    @Get("/list{?request*}")
    suspend fun listOrganization(@Valid request: ListDefaultedBusinessPartnersRequest): ResponseList<DefaultedBusinessPartnersResponse?> {
        return Response<ResponseList<DefaultedBusinessPartnersResponse?>>().ok(bprService.list(request))
    }
}
