package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.settlement.entity.GlCode
import com.cogoport.ares.api.settlement.repository.GlCodeRepository
import com.cogoport.ares.api.settlement.service.interfaces.GlCodeService
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class GlCodeServiceImpl : GlCodeService {

    @Inject
    lateinit var glCodeRepository: GlCodeRepository

    override suspend fun getGLCode(entityCode: Int?, q: String?, pageLimit: Int?): List<GlCode> {
        val updatedPageLimit = pageLimit ?: 10
        return glCodeRepository.getGLCode(entityCode, q, updatedPageLimit)
    }
}
