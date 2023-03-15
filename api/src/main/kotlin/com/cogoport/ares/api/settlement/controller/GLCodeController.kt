package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.entity.GLCodes
import com.cogoport.ares.api.settlement.service.interfaces.GLCodeService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/glcode")
class GLCodeController {

    @Inject
    lateinit var glCodeService: GLCodeService

    @Get
    suspend fun getGLCodeByEntity(@QueryValue("entityCode") entityCode: Int, @QueryValue("filters%5Bq%5D") query: String?): List<GLCodes> {
        return glCodeService.getGLCodeByEntity(entityCode)
    }
}
