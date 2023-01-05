package com.cogoport.ares.api.common.service.implementation

import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.OpenSearchService
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class Scheduler(private var openSearchService: OpenSearchService) {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Scheduled(cron = "0 0 * * *")
    suspend fun updateSupplierOutstandingOnOpenSearch() {
        runBlocking {
            val orgIds = accountUtilizationRepository.getSupplierOrgIds()
            val batches = orgIds.chunked(100)
            for (batch in batches) {
                openSearchService.updateSupplierOutstanding(batch)
            }
        }
    }
}
