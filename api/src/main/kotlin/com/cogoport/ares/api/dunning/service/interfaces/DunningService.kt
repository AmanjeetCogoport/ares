package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.api.dunning.model.request.CreateDunningException
import com.cogoport.ares.api.dunning.model.request.ListExceptionReq
import com.cogoport.ares.api.dunning.model.response.CycleWiseExceptionResp
import com.cogoport.ares.api.dunning.model.response.MasterExceptionResp
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.CreditControllerRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.ListDunningCycleExecutionReq
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import java.util.UUID

interface DunningService {
    suspend fun createCreditController(creditControllerRequest: CreditControllerRequest): Long
    suspend fun updateCreditController(updateCreditController: UpdateCreditControllerRequest): Long

    suspend fun createDunningCycle(createDunningCycleRequest: CreateDunningCycleRequest): Long

    suspend fun getCustomersOutstandingAndOnAccount(request: DunningCycleFilters): List<CustomerOutstandingAndOnAccountResponse>
    suspend fun listMasterException(request: ListExceptionReq): ResponseList<MasterExceptionResp>

    suspend fun createDunningException(request: CreateDunningException): MutableList<String>

    suspend fun getCycleWiseExceptions(request: ListExceptionReq): ResponseList<CycleWiseExceptionResp>

    suspend fun deleteOrUpdateMasterException(id: String, updatedBy: UUID, actionType: String): Boolean

    suspend fun listDunningCycleExecution(request: ListDunningCycleExecutionReq): List<DunningCycleExecution>
}
