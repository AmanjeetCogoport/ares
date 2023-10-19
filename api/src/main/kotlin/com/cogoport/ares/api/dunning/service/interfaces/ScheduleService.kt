package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.request.PaymentReminderReq
import com.cogoport.plutus.model.invoice.request.IrnGenerationEmailRequest

interface ScheduleService {

    suspend fun processCycleExecution(request: CycleExecutionProcessReq)

    suspend fun sendPaymentReminderToTradeParty(request: PaymentReminderReq)

    suspend fun sendEmailForIrnGeneration(request: IrnGenerationEmailRequest)
}
