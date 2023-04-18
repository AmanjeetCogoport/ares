package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.settlement.entity.GlCodeMaster
import com.cogoport.ares.api.settlement.repository.GlCodeMasterRepository
import com.cogoport.ares.api.settlement.service.interfaces.GlCodeMasterService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.enums.JVSageControls
import jakarta.inject.Inject

class GlCodeMasterServiceImpl : GlCodeMasterService {

    @Inject
    lateinit var glCodeMasterRepository: GlCodeMasterRepository

    @Inject
    lateinit var util: Util

    override suspend fun getGLCodeMaster(accMode: AccMode?, q: String?, pageLimit: Int?): List<GlCodeMaster> {
        val updatedPageLimit = pageLimit ?: 10
        var query = util.toQueryString(q)
        val updatedAccMode = when (accMode) {
            null -> " "
            else -> getAccModeValue(accMode)
        }
        return glCodeMasterRepository.getGLCodeMaster(updatedAccMode, query, updatedPageLimit)
    }

    private fun getAccModeValue(accMode: AccMode): String {
        val accMode = when (accMode) {
            AccMode.AP -> JVSageControls.AP.value
            AccMode.AR -> JVSageControls.AR.value
            AccMode.PDA -> JVSageControls.PDA.value
            AccMode.CSD -> JVSageControls.CSD.value
            AccMode.EMD -> JVSageControls.EMD.value
            AccMode.SUSA -> JVSageControls.SUSA.value
            AccMode.SUSS -> JVSageControls.SUSS.value
            else -> {
                throw AresException(AresError.ERR_1529, accMode.name)
            }
        }
        return accMode
    }
}
