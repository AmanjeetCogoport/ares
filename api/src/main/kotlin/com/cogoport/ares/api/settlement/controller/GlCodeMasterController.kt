package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.entity.GlCodeMaster
import com.cogoport.ares.api.settlement.service.interfaces.GlCodeMasterService
import com.cogoport.ares.model.payment.AccMode
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/gl-code-master")
class GlCodeMasterController {

    @Inject
    lateinit var glCodeMasterService: GlCodeMasterService

    @Get
    suspend fun getGLCodeMaster(@QueryValue("accMode") accMode: AccMode?, @QueryValue("q") q: String?, @QueryValue("pageLimit") pageLimit: Int? = 10): List<GlCodeMaster> {
        return glCodeMasterService.getGLCodeMaster(accMode, q, pageLimit)
    }
}
