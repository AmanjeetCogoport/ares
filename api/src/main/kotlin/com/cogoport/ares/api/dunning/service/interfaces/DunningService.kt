package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.model.dunning.request.CreditControllerRequest
import com.cogoport.ares.model.dunning.request.UpdateCreditControllerRequest

interface DunningService {

    suspend fun createCreditController(creditControllerRequest: CreditControllerRequest): Long
    suspend fun updateCreditController(updateCreditController: UpdateCreditControllerRequest): Long
}
