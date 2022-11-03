package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.api.payment.repository.BprRepository
import com.cogoport.ares.api.payment.service.interfaces.BprService
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.request.BprRequest
import com.cogoport.ares.model.payment.request.ListBprRequest
import com.cogoport.ares.model.payment.response.BprResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import javax.validation.Valid

@Validated
@Controller("/bpr")
class BprController {

    @Inject
    lateinit var bprService: BprService

    @Inject
    lateinit var bprRepository: BprRepository

    @Post
    suspend fun addBpr(@Valid @Body request: BprRequest): Response<Long> {
        return Response<Long>().ok(HttpStatus.CREATED.name, bprService.add(request))
    }

    @Post("/delete/{id}")
    suspend fun remove(@PathVariable("id") id: Long): Response<Long> {
        return Response<Long>().ok("Deleted", bprRepository.delete(id))
    }

    @Get("/list{?request*}")
    suspend fun listBpr(@Valid request: ListBprRequest): ResponseList<BprResponse?> {
        return Response<ResponseList<BprResponse?>>().ok(bprService.list(request))
    }
}
