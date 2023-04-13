package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.entity.JvCategory
import com.cogoport.ares.api.settlement.service.interfaces.JvCategoryService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/jv-category")
class JvCategoryController {

    @Inject
    lateinit var jvCategoryService: JvCategoryService

    @Get
    suspend fun getJVCategories(@QueryValue("q") q: String?, @QueryValue("pageLimit") pageLimit: Int? = 10): List<JvCategory> {
        return jvCategoryService.getJvCategory(q, pageLimit)
    }
}
