package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.entity.GLCode

interface GLCodeService {
    suspend fun getGLCodeByEntity(entityCode: Int): List<GLCode>
}
