package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.settlement.repository.GLCodeRepo
import com.cogoport.ares.api.settlement.service.interfaces.GLCodeService
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class GLCodeImpl: GLCodeService {

    @Inject
    lateinit var glCodeRepo: GLCodeRepo

    override suspend fun getGLCodeByEntity(entityCode: Int): List<String> {
        return glCodeRepo.getGLCodes(entityCode)
    }
}
