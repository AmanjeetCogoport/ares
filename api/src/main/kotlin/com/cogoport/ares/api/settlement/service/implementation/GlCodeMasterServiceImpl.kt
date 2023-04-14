package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.settlement.entity.GlCodeMaster
import com.cogoport.ares.api.settlement.entity.JvCategory
import com.cogoport.ares.api.settlement.repository.GlCodeMasterRepository
import com.cogoport.ares.api.settlement.service.interfaces.GlCodeMasterService
import com.cogoport.ares.model.payment.AccMode
import jakarta.inject.Inject

class GlCodeMasterServiceImpl: GlCodeMasterService {

    @Inject
    lateinit var glCodeMasterRepository: GlCodeMasterRepository

    override suspend fun getGLCodeMaster(accMode: AccMode?, q: String?, pageLimit: Int?): List<GlCodeMaster> {
        val updatedPageLimit = pageLimit ?: 10
        return glCodeMasterRepository.getGLCodeMaster(q, updatedPageLimit)
    }
}