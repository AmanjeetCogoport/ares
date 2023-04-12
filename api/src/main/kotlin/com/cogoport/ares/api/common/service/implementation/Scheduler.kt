package com.cogoport.ares.api.common.service.implementation

import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.UnifiedDBRepo
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import io.micronaut.scheduling.annotation.Scheduled
import io.sentry.Sentry
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class Scheduler(
    private var emitter: AresMessagePublisher,
    private var accountUtilizationRepository: AccountUtilizationRepository,
    private var outStandingService: OutStandingService,
    private var unifiedDBRepo: UnifiedDBRepo
) {

    @Scheduled(cron = "0 0 * * *")
    fun updateSupplierOutstandingOnOpenSearch() {
        runBlocking {
            val orgIds = accountUtilizationRepository.getTradePartyOrgIds(AccMode.AP)
            for (orgId in orgIds) {
                try {
                    emitter.emitUpdateSupplierOutstanding(UpdateSupplierOutstandingRequest(orgId = orgId))
                } catch (e: Exception) {
                    logger().error(e.message)
                    Sentry.captureException(e)
                }
            }
        }
    }

    @Scheduled(cron = "0 0 * * *")
    fun updateCustomerOutstandingOnOpenSearch() {
        runBlocking {
            val orgIds = accountUtilizationRepository.getTradePartyOrgIds(AccMode.AR)
            for (orgId in orgIds) {
                try {
                    emitter.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(orgId = orgId))
                } catch (e: Exception) {
                    logger().error(e.message)
                    Sentry.captureException(e)
                }
            }
        }
    }
    @Scheduled(cron = "30 04 * * *")
    fun uploadPayblesInfo() {
        runBlocking {
            outStandingService.uploadPayblesStats()
        }
    }

    @Scheduled(cron = "0 * * * *")
    fun deleteInvoicesNotPresentInPlutus() {
        runBlocking {
            val ids = unifiedDBRepo.getInvoicesNotPresentInPlutus()
            if (!ids.isNullOrEmpty()) {
                for (id in ids) {
                    emitter.emitDeleteInvoicesNotPresentInPlutus(id)
                }
            }
        }
    }

    @Scheduled(cron = "0 0 * * *")
    fun saveDailyOnAccountAndOutstandingOnRedis() {
        runBlocking {
        emitter.emitSaveDailyOnAccountAndOutstandingOnRedis()
        }
    }
}
