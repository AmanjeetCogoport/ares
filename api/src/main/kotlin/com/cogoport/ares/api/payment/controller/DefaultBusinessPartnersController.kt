package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.service.interfaces.DefaultBusinessPartnersService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.BprRequest
import com.cogoport.ares.model.payment.request.ListBprRequest
import com.cogoport.ares.model.payment.response.DefaultBusinessPartnersResponse
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
class DefaultBusinessPartnersController {

    @Inject
    lateinit var bprService: DefaultBusinessPartnersService

    @Post
    suspend fun addBpr(@Valid @Body request: BprRequest): Response<Long> {
        return Response<Long>().ok("Saved", bprService.add(request))
    }

    @Delete("/delete/{id}")
    suspend fun remove(@PathVariable("id") id: Long): Response<Long> {
        return Response<Long>().ok("Deleted", bprService.delete(id))
    }

    @Get("/list{?request*}")
    suspend fun listBpr(@Valid request: ListBprRequest): ResponseList<DefaultBusinessPartnersResponse?> {
        return Response<ResponseList<DefaultBusinessPartnersResponse?>>().ok(bprService.list(request))
    }
}
