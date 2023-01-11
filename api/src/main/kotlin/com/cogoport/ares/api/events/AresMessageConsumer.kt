package com.cogoport.ares.api.events

import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking

@RabbitListener
class AresMessageConsumer {
    @Inject
    lateinit var outstandingService: OutStandingService
    @Queue("update-supplier-details", prefetch = 10)
    fun updateSupplierOutstanding(request: UpdateSupplierOutstandingRequest) = runBlocking {
        outstandingService.updateSupplierDetails("9057b0cf-ada9-4263-9f6f-12d233c4ace9", false, null)
    }
}
