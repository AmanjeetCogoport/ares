package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.entity.GlCode

interface GlCodeService {

    suspend fun getGLCode(entityCode: Int?, q: String?): List<GlCode>
}
