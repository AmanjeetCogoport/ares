package com.cogoport.ares.api.dunning.controller

import com.cogoport.ares.api.dunning.model.request.ListMasterExceptionReq
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.model.common.ResponseList
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import javax.validation.Valid

@Controller("/dunning")
class DunningController (
    private val dunningService: DunningService
        ){

    @Get("/master-exceptions{?request*}")
    suspend fun listMasterException(@Valid  request: ListMasterExceptionReq): ResponseList<MasterExceptionResp> {
        return dunningService.listMasterException(request)
    }
}
