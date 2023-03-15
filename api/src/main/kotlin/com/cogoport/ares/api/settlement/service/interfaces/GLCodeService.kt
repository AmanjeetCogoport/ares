package com.cogoport.ares.api.settlement.service.interfaces

interface GLCodeService {
    suspend fun getGLCodeByEntity(entityCode: Int): List<String>
}
