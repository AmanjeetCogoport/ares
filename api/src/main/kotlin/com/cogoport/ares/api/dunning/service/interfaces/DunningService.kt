package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.ListDunningCycleReq
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.ListCreditControllerRequest
import com.cogoport.ares.model.dunning.request.ListDunningCycleExecutionReq
import com.cogoport.ares.model.dunning.request.SyncOrgStakeholderRequest
import com.cogoport.ares.model.dunning.request.UpdateCycleExecutionRequest
import com.cogoport.ares.model.dunning.request.UpdateDunningCycleExecutionStatusReq
import com.cogoport.ares.model.dunning.response.CreditControllerResponse
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.dunning.response.DunningCycleExecutionResponse
import com.cogoport.ares.model.dunning.response.DunningCycleResponse
import java.util.UUID

interface DunningService {
    suspend fun syncOrgStakeholders(syncOrgStakeholderRequest: SyncOrgStakeholderRequest): Long

    suspend fun createDunningCycle(createDunningCycleRequest: CreateDunningCycleRequest): Long

    suspend fun getCustomersOutstandingAndOnAccount(request: DunningCycleFilters): ResponseList<CustomerOutstandingAndOnAccountResponse>
    suspend fun listMasterException(request: ListExceptionReq): ResponseList<MasterExceptionResp>

    suspend fun createDunningException(request: CreateDunningException): MutableList<String>

    suspend fun getCycleWiseExceptions(request: ListExceptionReq): ResponseList<CycleWiseExceptionResp>

    suspend fun deleteOrUpdateMasterException(id: String, updatedBy: UUID, actionType: String): Boolean

    suspend fun deleteCycle(id: String, updatedBy: UUID): Boolean

    suspend fun updateStatusDunningCycle(updateDunningCycleExecutionStatusReq: UpdateDunningCycleExecutionStatusReq): Boolean

    suspend fun listDunningCycles(request: ListDunningCycleReq): ResponseList<DunningCycleResponse>

    suspend fun listDunningCycleExecution(request: ListDunningCycleExecutionReq): ResponseList<DunningCycleExecutionResponse>

    suspend fun updateCycleExecution(request: UpdateCycleExecutionRequest): Long

    suspend fun listDistinctCreditControllers(request: ListCreditControllerRequest): List<CreditControllerResponse>
}
