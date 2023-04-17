package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.api.settlement.entity.GlCodeMaster
import com.cogoport.ares.model.payment.AccMode

interface GlCodeMasterService {

    suspend fun getGLCodeMaster(accMode: AccMode?, q: String?, pageLimit: Int?): List<GlCodeMaster>
}
