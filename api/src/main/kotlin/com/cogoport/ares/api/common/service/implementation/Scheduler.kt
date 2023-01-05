package com.cogoport.ares.api.common.service.implementation

import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.utils.logger
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class Scheduler(private var outStandingService: OutStandingService, private var accountUtilizationRepository: AccountUtilizationRepository) {

    @Scheduled(cron = "* * * * * *")
    fun updateSupplierOutstandingOnOpenSearch() {
        runBlocking {
            val orgIds = accountUtilizationRepository.getSupplierOrgIds()
            for (id in orgIds) {
                try {
                    outStandingService.updateSupplierOutstanding(id)
                } catch (e: Exception) {
                    logger().error("error while running cron for updating supplier outstanding")
                }
            }
        }
    }
}
