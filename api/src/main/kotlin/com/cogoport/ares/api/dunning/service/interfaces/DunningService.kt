package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.model.common.ResponseList
import java.util.UUID

interface DunningService {
    suspend fun listMasterException(request: ListExceptionReq): ResponseList<MasterExceptionResp>

    suspend fun createDunningException(request: CreateDunningException): MutableList<String>

    suspend fun getCycleWiseExceptions(request: ListExceptionReq): ResponseList<CycleWiseExceptionResp>

    suspend fun deleteOrUpdateMasterException(id: String, updatedBy: UUID, actionType: String): Boolean

//    suspend fun listDunningCycles(request: ListDunningCycleReq): ResponseList<ListDunningCycleResp>
}
