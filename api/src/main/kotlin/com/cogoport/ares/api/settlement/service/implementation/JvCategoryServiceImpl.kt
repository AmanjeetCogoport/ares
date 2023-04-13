package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.settlement.entity.JvCategory
import com.cogoport.ares.api.settlement.repository.JvCategoryRepository
import com.cogoport.ares.api.settlement.service.interfaces.JvCategoryService
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class JvCategoryServiceImpl : JvCategoryService {

    @Inject
    lateinit var jvCategoryRepository: JvCategoryRepository

    override suspend fun getJvCategory(q: String?, pageLimit: Int?): List<JvCategory> {
        val updatedPageLimit = pageLimit ?: 10
        return jvCategoryRepository.getJvCategory(q, updatedPageLimit)
    }
}
