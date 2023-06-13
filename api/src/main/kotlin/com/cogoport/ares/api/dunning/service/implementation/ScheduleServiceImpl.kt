package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.repository.DunningCycleExceptionRepo
import com.cogoport.ares.api.dunning.service.interfaces.ScheduleService
import com.cogoport.ares.model.dunning.enum.CycleExecutionStatus
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Singleton

@Singleton
class ScheduleServiceImpl(
    private val dunningExecutionRepo: DunningCycleExceptionRepo
) : ScheduleService {

    override suspend fun processCycleExecution(request: CycleExecutionProcessReq) {
        val executionId = Hashids.decode(request.scheduleId)[0]
        val executionDetails = dunningExecutionRepo.findById(executionId)
        if(executionDetails == null || executionDetails.deletedAt != null){
            return
        }
        dunningExecutionRepo.updateStatus(executionId, CycleExecutionStatus.IN_PROGRESS.name)
        try {

        }

    }

}