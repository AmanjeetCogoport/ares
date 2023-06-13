package com.cogoport.ares.api.dunning.service.interfaces

import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq

interface ScheduleService {

    suspend fun processCycleExecution(request: CycleExecutionProcessReq)

}