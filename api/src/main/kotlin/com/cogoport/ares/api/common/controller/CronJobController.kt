package com.cogoport.ares.api.common.controller

import com.cogoport.ares.api.common.service.interfaces.CronJobService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject

@Controller("cron-jobs")
class CronJobController {

    @Inject
    lateinit var cronJobService: CronJobService

    @Get("sales-amount-mismatch")
    suspend fun getSalesAmountMismatchInJobs(): List<Long>? {
        return cronJobService.getSalesAmountMismatchInJobs()
    }

    @Get("purchase-amount-mismatch")
    suspend fun getPurchaseAmountMismatchInJobs(): List<Long>? {
        return cronJobService.getPurchaseAmountMismatchInJobs()
    }
}
