package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking

@RabbitListener
class AresMessageConsumer {

    @Inject
    lateinit var outstandingService: OutStandingService

    @Inject
    private lateinit var settlementService: SettlementService

    @Queue("update-supplier-details", reQueue = true, prefetch = 1)
    fun updateSupplierOutstanding(request: UpdateSupplierOutstandingRequest) = runBlocking {
        outstandingService.updateSupplierDetails(request.orgId.toString(), false, null)
    }

    @Queue("unfreeze-credit-consumption", reQueue = true, prefetch = 1)
    fun unfreezeCreditConsumption(request: Settlement) = runBlocking {
        settlementService.sendKnockOffDataToCreditConsumption(request)
    }
}
