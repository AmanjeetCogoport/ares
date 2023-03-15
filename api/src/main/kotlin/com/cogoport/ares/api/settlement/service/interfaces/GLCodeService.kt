package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.entity.GLCodes

interface GLCodeService {
    suspend fun getGLCodeByEntity(entityCode: Int): List<GLCodes>
}
