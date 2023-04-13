package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.entity.GlCode
import com.cogoport.ares.api.settlement.service.interfaces.GlCodeService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/gl-code")
class GlCodeController {

    @Inject
    lateinit var glCodeService: GlCodeService

    @Get
    suspend fun getGLCode(@QueryValue("entityCode") entityCode: Int?, @QueryValue("q") q: String?): List<GlCode> {
        return glCodeService.getGLCode(entityCode, q)
    }
}
