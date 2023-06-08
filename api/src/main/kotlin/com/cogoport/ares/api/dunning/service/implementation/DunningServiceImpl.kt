package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.dunning.model.request.ListMasterExceptionReq
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.api.dunning.repository.CycleExceptionRepo
import com.cogoport.ares.api.dunning.service.interfaces.DunningService
import com.cogoport.ares.model.common.ResponseList
import jakarta.inject.Singleton

@Singleton
class DunningServiceImpl(
    private val cycleExceptionRepo: CycleExceptionRepo
) : DunningService {
    override suspend fun listMasterException(request: ListMasterExceptionReq): ResponseList<MasterExceptionResp> {
       val responseList = cycleExceptionRepo
    }
}
