package com.cogoport.ares.api.common.service.interfaces

interface CronJobService {
    suspend fun getSalesAmountMismatchInJobs(): List<Long>?
    suspend fun getPurchaseAmountMismatchInJobs(): List<Long>?
}
