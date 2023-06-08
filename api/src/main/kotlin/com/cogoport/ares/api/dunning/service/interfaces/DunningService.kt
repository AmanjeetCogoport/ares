package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.api.dunning.model.request.ListMasterExceptionReq
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.model.common.ResponseList

interface DunningService{
    suspend fun listMasterException(request: ListMasterExceptionReq): ResponseList<MasterExceptionResp>
}
