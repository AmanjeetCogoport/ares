package com.cogoport.ares.api.common.service.implementation

import com.cogoport.ares.api.common.service.interfaces.CronJobService
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import jakarta.inject.Inject

class CronJobImplementation : CronJobService {

    @Inject
    lateinit var unifiedDBRepo: UnifiedDBRepo

    override suspend fun getSalesAmountMismatchInJobs(): List<Long>? {
        return unifiedDBRepo.getSalesAmountMismatchInJobs()
    }

    override suspend fun getPurchaseAmountMismatchInJobs(): List<Long>? {
        return unifiedDBRepo.getPurchaseAmountMismatchInJobs()
    }
}
