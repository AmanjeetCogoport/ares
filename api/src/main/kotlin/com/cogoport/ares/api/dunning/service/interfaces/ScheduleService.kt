package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.model.request.PaymentReminderReq

interface ScheduleService {

    suspend fun processCycleExecution(request: CycleExecutionProcessReq)

    suspend fun sendPaymentReminderToTradeParty(request: PaymentReminderReq)
}
