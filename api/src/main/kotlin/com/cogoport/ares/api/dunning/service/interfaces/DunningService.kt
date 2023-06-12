package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.model.dunning.request.CreateDunningCycleRequest
import com.cogoport.ares.model.dunning.request.CreditControllerRequest
import com.cogoport.ares.model.dunning.request.DunningCycleFilters
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse

interface DunningService {

    suspend fun createCreditController(creditControllerRequest: CreditControllerRequest): Long
    suspend fun updateCreditController(updateCreditController: UpdateCreditControllerRequest): Long

    suspend fun createDunningCycle(createDunningCycleRequest: CreateDunningCycleRequest): Long

    suspend fun getCustomersOutstandingAndOnAccount(request: DunningCycleFilters): List<CustomerOutstandingAndOnAccountResponse>
}
